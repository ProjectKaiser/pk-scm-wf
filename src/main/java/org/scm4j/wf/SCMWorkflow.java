package org.scm4j.wf;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.scm4j.wf.actions.ActionError;
import org.scm4j.wf.actions.ActionKind;
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
		return getProductionReleaseAction(new Component(componentName, repos), ActionKind.AUTO);
	}
	
	public IAction getProductionReleaseAction(String componentName, ActionKind actionKind) {
		return getProductionReleaseAction(new Component(componentName, repos), actionKind);
	}
	
	public IAction getProductionReleaseAction(Component comp) {
		return getProductionReleaseAction(comp, ActionKind.AUTO);
	}
	
	public IAction getProductionReleaseAction(Component comp, ActionKind actionKind) {
		List<IAction> childActions = new ArrayList<>();
		DevelopBranch db = new DevelopBranch(comp);
		List<Component> devMDeps = db.getMDeps();
		
		for (Component mDep : devMDeps) {
			childActions.add(getProductionReleaseAction(mDep, actionKind));
		}
		
		return getProductionReleaseActionRoot(comp, childActions, actionKind);
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
			rb = new ReleaseBranch(comp, new Version(ver.toPreviousMinor().toReleaseString()), repos);
		}
		return null;
	}
	
	public IAction getProductionReleaseActionRoot(Component comp, List<IAction> childActions, ActionKind actionKind) {
		DevelopBranch db = new DevelopBranch(comp);
		if (!db.hasVersionFile()) {
			throw new EComponentConfig("no " + VER_FILE_NAME + " file for " + comp.toString());
		}
		
		if (hasErrorActions(childActions)) {
			return new ActionNone(comp, childActions, "has child error actions ");
		}
		
		DevelopBranchStatus dbs = db.getStatus();
		ReleaseBranch rb = db.getCurrentReleaseBranch(repos); //getLastUnbuiltReleaseBranch(comp);
		ReleaseBranchStatus rbs = rb.getStatus();
		if (dbs == DevelopBranchStatus.MODIFIED) {
			if (rbs == ReleaseBranchStatus.MISSING || rbs == ReleaseBranchStatus.BUILT || rbs == ReleaseBranchStatus.TAGGED) {
				// if we are MODIFIED and RB is MISSING or completed then need to fork a new release
				skipAllBuilds(childActions);
				if (actionKind == ActionKind.BUILD) {
					return new ActionNone(comp, childActions, "nothing to build");
				} 
				return new SCMActionForkReleaseBranch(comp, childActions, ReleaseReason.NEW_FEATURES);
			}
			// if we are MODIFIED and RB is not completed then need to accomplish the existsing RB
			skipAllForks(childActions);
			if (actionKind == ActionKind.FORK) {
				return new ActionNone(comp, childActions, "nothing to fork");
			}
			return new SCMActionBuild(comp, childActions, ReleaseReason.NEW_FEATURES, rb.getVersion());
		}
		
		// If BRANCHED then surely forked. And if IGNORED then surely not forked
		
		if (dbs == DevelopBranchStatus.BRANCHED) {
			/**
			 * this means we are surely forked
			 */
			
			if (rbs == ReleaseBranchStatus.MISSING) {
				// this means that weare just forked a branch and set #scm-ver tag in trunk. We are trying to determine the current release branch which does not exist yet.
				// Is this possible? Possible: forked 2.59, 2.60 is written to trunk => dbs is BRANCHED, rbs is MISSING
				return new ActionNone(comp, childActions, "");
			}
			if (rbs == ReleaseBranchStatus.TAGGED || rbs == ReleaseBranchStatus.BUILT) {
				return getActionIfNewDependencies(comp, childActions, rb, actionKind);
			}
			// if a Release branch is not completed, i.e. MDEPS_* or BRANCHED then need to build the unbuilt
			skipAllForks(childActions);
			if (actionKind == ActionKind.FORK) {
				return new ActionNone(comp, childActions, "nothing to fork");
			}
			return rb.getMDeps().isEmpty() ? 
					new SCMActionBuild(comp, childActions, ReleaseReason.NEW_FEATURES, rb.getVersion()) :
					new SCMActionBuild(comp, childActions, ReleaseReason.NEW_DEPENDENCIES, rb.getVersion());
		}
		
		return  getActionIfNewDependencies(comp, childActions, rb, actionKind); 
	}

	

	private IAction getActionIfNewDependencies(Component comp, List<IAction> childActions, ReleaseBranch lastUnbuiltRB, ActionKind actionKind) {
		
		/**
		 * Will check previous release for each mDep. If it exists then check if this release was used in the current (here it is the last unbuilt) version of the current component?
		 */
		
		List<Component> mDepsFromDev = new DevelopBranch(comp).getMDeps();
		List<Component> mDepsFromLastUnbuiltRB = lastUnbuiltRB.getMDeps();
		for (Component mDepFromDev : mDepsFromDev) {
			/**
			 * Take the last UDB release
			 */
			ReleaseBranch lastMDepRelease = getLastForkedReleaseBranch(mDepFromDev);
			if (lastMDepRelease == null) {
				// UDB was not built
				// let's see is NEW_DEPENDENCIES required due of we just forked UDB?
				continue;
			}
			if (mDepsFromLastUnbuiltRB.isEmpty()) {
				/**
				 * If we are use nothing and UDB release exists then we have NEW_DEPENDENCIES
				 * Fork only is possible here. Build is impossible because release branch with correct mDeps does not exists
				 */
				skipAllBuilds(childActions);
				if (actionKind == ActionKind.BUILD) {
					return new ActionNone(comp, childActions, "nothing to build");
				}
				return new SCMActionForkReleaseBranch(comp, childActions, ReleaseReason.NEW_DEPENDENCIES);
			}
			// The last UDB release found. Check if this release is used in lastUnbuiltRB
			for (Component mDepFromLastUnbuiltRB : mDepsFromLastUnbuiltRB) {
				if (mDepFromLastUnbuiltRB.getName().equals(mDepFromDev.getName()) && !mDepFromLastUnbuiltRB.getVersion().equals(lastMDepRelease.getVersion())) {
					//  Fork only is possible here. Build is impossible because release branch with correct mDeps does not exists
					skipAllBuilds(childActions);
					if (actionKind == ActionKind.BUILD) {
						return new ActionNone(comp, childActions, "nothing to build");
					}
					return new SCMActionForkReleaseBranch(comp, childActions, ReleaseReason.NEW_DEPENDENCIES);
				}
			}
		}
		
		return new ActionNone(comp, childActions, "already built");
	}

	private void skipAllForks(List<IAction> childActions) {
		ListIterator<IAction> li = childActions.listIterator();
		IAction action;
		while (li.hasNext()) {
			action = li.next();
			if (action instanceof SCMActionForkReleaseBranch) {
				li.set(new ActionNone(((SCMActionForkReleaseBranch) action).getComponent(), action.getChildActions(), "fork skipped because not all parent components built"));
			}
		}
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
