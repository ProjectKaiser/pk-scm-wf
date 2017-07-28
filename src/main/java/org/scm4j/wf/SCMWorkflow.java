package org.scm4j.wf;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.scm4j.wf.actions.ActionError;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.DevelopBranch;
import org.scm4j.wf.branch.DevelopBranchStatus;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.branch.ReleaseBranchStatus;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.exceptions.EComponentConfig;
import org.scm4j.wf.scmactions.ReleaseReason;
import org.scm4j.wf.scmactions.SCMActionBuild;
import org.scm4j.wf.scmactions.SCMActionForkReleaseBranch;
import org.scm4j.wf.scmactions.SCMActionTagRelease;
import org.scm4j.wf.scmactions.SCMActionUseExistingTag;

public class SCMWorkflow implements ISCMWorkflow {

	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String MDEPS_CHANGED_FILE_NAME = "mdeps-changed";

	private final VCSRepositories repos;
	
	public SCMWorkflow(VCSRepositories repos) {
		this.repos = repos;
	}
	
	public SCMWorkflow() {
		this(VCSRepositories.loadVCSRepositories());
	}
	
	@Override 
	public IAction getProductionReleaseAction(String componentName) {
		return getProductionReleaseAction(new Component(componentName, repos));
	}
	
	public IAction getProductionReleaseAction(Component comp) {
		List<IAction> childActions = new ArrayList<>();
		DevelopBranch db = new DevelopBranch(comp);
		List<Component> devMDeps = db.getMDeps();
		
		for (Component mDep : devMDeps) {
			childActions.add(getProductionReleaseAction(mDep));
		}
		
		return getProductionReleaseActionRoot(comp, childActions);
	}
	
	public ReleaseBranch getLastForkedReleaseBranch(Component comp) {
		DevelopBranch db = new DevelopBranch(comp);
		Version ver = db.getVersion();
		
		ReleaseBranch rb = new ReleaseBranch(comp, repos);
		for (int i = 0; i <= 1; i++) {
			ReleaseBranchStatus rbs = rb.getStatus();
			if (rbs == ReleaseBranchStatus.BRANCHED || rbs == ReleaseBranchStatus.BUILT || rbs == ReleaseBranchStatus.TAGGED) {
				return rb;
			}
			rb = new ReleaseBranch(comp, new Version(ver.toPreviousMinorRelease()), repos);
		}
		return null;
	}
	
	public ReleaseBranch getLastUnbuiltReleaseBranch(Component comp) {
		DevelopBranch db = new DevelopBranch(comp);
		Version ver = db.getVersion();
		
		ReleaseBranch rb = new ReleaseBranch(comp, new Version(ver.toPreviousMinorRelease()), repos);
		ReleaseBranch oldestRB = null;
		for (int i = 0; i <= 1; i++) {
			ReleaseBranchStatus rbs = rb.getStatus();
			
			if (rbs != ReleaseBranchStatus.MISSING && rbs != ReleaseBranchStatus.BUILT && rbs != ReleaseBranchStatus.TAGGED) {
				oldestRB = rb;
			}
			rb = new ReleaseBranch(comp, new Version(ver.toPreviousMinorRelease()), repos);
		}
		return oldestRB != null ? oldestRB : new ReleaseBranch(comp, repos);
	}
	
	public IAction getProductionReleaseActionRoot(Component comp, List<IAction> childActions) {
		DevelopBranch db = new DevelopBranch(comp);
		if (!db.hasVersionFile()) {
			throw new EComponentConfig("no " + VER_FILE_NAME + " file for " + comp.toString());
		}
		
		if (hasErrorActions(childActions)) {
			return new ActionNone(comp, childActions, "has child error actions   ");
		}
		
		ReleaseBranch rb = getLastUnbuiltReleaseBranch(comp);
		DevelopBranchStatus dbs = db.getStatus();
		if (dbs == DevelopBranchStatus.MODIFIED) {
			/**
			 * будем считать так: если мы MODIFIED, то значит мы по-любому не forked. Будем форкаться.
			 */
			skipAllBuilds(childActions);
			return new SCMActionForkReleaseBranch(comp, childActions, ReleaseReason.NEW_FEATURES);
		}
		
		// тут если BRANCHED, то по-любому forked. А если IGNORED, то по-любому не forked
		
		if (dbs == DevelopBranchStatus.BRANCHED) {
			/**
			 * это значит мы по-любому forked.
			 */
			ReleaseBranchStatus rbs = rb.getStatus();
			if (rbs == ReleaseBranchStatus.MISSING) {
				// это значит мы только-только отвели ветку и поставили #scm-ver в транк. Мы пытаемся определить ветку текущего релиза, которой еще нет.
				return new ActionNone(comp, childActions, "");
			}
			if (rbs == ReleaseBranchStatus.TAGGED || rbs == ReleaseBranchStatus.BUILT) {
				return getActionIfNewDependencies(comp, childActions, rb);
			}
			// если релизная ветка в подвешенном состоянии, т.е. MDEPS_* или BRANCHED, то достраиваем недостроенное
			// здесь не может быть NEW_FEATURES, т.к. тогда бы dbs была бы MODIFIED.
			return rb.getMDeps().isEmpty() ? 
					new SCMActionBuild(comp, childActions, ReleaseReason.NEW_FEATURES, rb.getVersion()) :
					new SCMActionBuild(comp, childActions, ReleaseReason.NEW_DEPENDENCIES, rb.getVersion());
			
		}
		
		return  getActionIfNewDependencies(comp, childActions, rb); 
	}

	private IAction getActionIfNewDependencies(Component comp, List<IAction> childActions,
			ReleaseBranch lastUnbuiltRB) {
		
		/**
		 * для каждого mDep посмотрим его предыдущий релиз. Если он есть, то посмотрим, а использовался ли этот mDeps в данной (она здесь последняя unbuilt) версии данного компонента?
		 */
		
		List<Component> mDepsFromDev = new DevelopBranch(comp).getMDeps();
		List<Component> mDepsFromLastUnbuiltRB = lastUnbuiltRB.getMDeps();
		for (Component mDepFromDev : mDepsFromDev) {
			/**
			 * берем последний релиз UDB. 
			 */
			ReleaseBranch lastMDepRelease = getLastForkedReleaseBranch(mDepFromDev);
			if (lastMDepRelease == null) {
				// это значит udb не собирался.
				// посмотрим, а не надо ли NEW_DEPENDENCIES из-за того, что мы только что отвели UDB?
				continue;
			}
			if (mDepsFromLastUnbuiltRB.isEmpty()) {
				// если у нас вообще ничего не используется, а релиз UDB есть - значит есть NEW_DEPENDENCIES
				return getForkOrBuildAction(comp, childActions, lastUnbuiltRB);
			}
			// нашли последний релиз UDB. Посмотрим, используется ли этот релиз в lastUnbuiltRB
			for (Component mDepFromLastUnbuiltRB : mDepsFromLastUnbuiltRB) {
				if (mDepFromLastUnbuiltRB.getName().equals(mDepFromDev.getName()) && !mDepFromLastUnbuiltRB.getVersion().equals(lastMDepRelease.getVersion())) {
					return getForkOrBuildAction(comp, childActions, lastUnbuiltRB);  
				}
			}
		}
		
		return new ActionNone(comp, childActions, null);
	}

	private IAction getForkOrBuildAction(Component comp, List<IAction> childActions, ReleaseBranch rb) {
		ReleaseBranchStatus rbs = rb.getStatus();
		if (rbs == ReleaseBranchStatus.MISSING) {
			skipAllBuilds(childActions);
			return new SCMActionForkReleaseBranch(comp, childActions, ReleaseReason.NEW_DEPENDENCIES);
		}
		return new SCMActionBuild(comp, childActions, ReleaseReason.NEW_DEPENDENCIES, rb.getVersion());
	}

	private void skipAllBuilds(List<IAction> childActions) {		
		ListIterator<IAction> li = childActions.listIterator();
		IAction action;
		while (li.hasNext()) {
			action = li.next();
			if (action instanceof SCMActionBuild) {
				li.set(new ActionNone(((SCMActionBuild) action).getComponent(), action.getChildActions(), "build skipped because not all parent components forked"));
			}
		}
	}


	private boolean hasErrorActions(List<IAction> actions) {
		for (IAction action : actions) {
			if (action instanceof ActionError) {
				return true;
			}
		}
		return false;
	}

//	private boolean hasSignificantActions(List<IAction> actions) {
//		for (IAction action : actions) {
//			if (!(action instanceof ActionNone) && !(action instanceof SCMActionUseLastReleaseVersion)) {
//				return true;
//			}
//		}
//		return false;
//	}

	@Override
	public IAction getTagReleaseAction(Component comp) {
		List<IAction> childActions = new ArrayList<>();
		DevelopBranch db = new DevelopBranch(comp);
		List<Component> mDeps = db.getMDeps();
		
		for (Component mDep : mDeps) {
			childActions.add(getTagReleaseAction(mDep));
		}
		return getTagReleaseActionRoot(comp, childActions);
	}

	private IAction getTagReleaseActionRoot(Component comp, List<IAction> childActions) {
		ReleaseBranch rb = new ReleaseBranch(comp, repos);
		if (rb.getStatus() == ReleaseBranchStatus.TAGGED) {
			return new SCMActionUseExistingTag(comp, childActions, rb.getReleaseTag());
		} else {
			return new SCMActionTagRelease(comp, childActions, "tag message");
		}
	}
}
