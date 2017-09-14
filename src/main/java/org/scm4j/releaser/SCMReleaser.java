package org.scm4j.releaser;

import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.DevelopBranchStatus;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.branch.ReleaseBranchStatus;
import org.scm4j.releaser.conf.*;
import org.scm4j.releaser.exceptions.EComponentConfig;
import org.scm4j.releaser.scmactions.ReleaseReason;
import org.scm4j.releaser.scmactions.SCMActionBuild;
import org.scm4j.releaser.scmactions.SCMActionFork;
import org.scm4j.releaser.scmactions.SCMActionTagRelease;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSTag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class SCMReleaser {

	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String DELAYED_TAGS_FILE_NAME = "delayed-tags.yml"; 
	public static final File BASE_WORKING_DIR = new File(System.getProperty("user.home"), ".scm4j");

	private final List<Option> options;
	
	public SCMReleaser(List<Option> options) {
		this.options = options;
	}
	
	public SCMReleaser() {
		this(new ArrayList<Option>());
	}
	
	public static List<Option> parseOptions(String[] args) {
		List<Option> options = new ArrayList<>();
		for (String arg : args) {
			if (Option.getArgsMap().containsKey(arg)) {
				options.add(Option.getArgsMap().get(arg));
			}
		}
		return options;
	}
	
	public static File getDelayedTagsFile() {
		return new File(SCMReleaser.DELAYED_TAGS_FILE_NAME);
	}
	
	public IAction getProductionReleaseAction(String componentName) {
		return getProductionReleaseAction(new Component(componentName, true), ActionKind.AUTO);
	}
	
	public IAction getProductionReleaseAction(String componentCoords, ActionKind actionKind) {
		return getProductionReleaseAction(new Component(componentCoords, true), actionKind);
	}
	
	public IAction getProductionReleaseAction(Component comp) {
		return getProductionReleaseAction(comp, ActionKind.AUTO);
	}

	public IAction getProductionReleaseAction(Component comp, ActionKind actionKind) {
		List<IAction> childActions = new ArrayList<>();
		ReleaseBranch rb = new ReleaseBranch(comp);
		List<Component> mDeps;
		if (!rb.exists()) {
			DevelopBranch db = new DevelopBranch(comp);
			mDeps = db.getMDeps();
		} else {
			mDeps = rb.getMDeps();
		}

		for (Component mDep : mDeps) {
			childActions.add(getProductionReleaseAction(mDep, actionKind)); 
		}

		return getProductionReleaseActionRoot(comp, childActions, actionKind);
	}
		
	public IAction getProductionReleaseActionRoot(Component comp, List<IAction> childActions, ActionKind actionKind) {
		DevelopBranch db = new DevelopBranch(comp);
		if (!db.hasVersionFile()) {
			throw new EComponentConfig("no " + VER_FILE_NAME + " file for " + comp.toString());
		}
		DevelopBranchStatus dbs = db.getStatus();

		if (dbs == DevelopBranchStatus.IGNORED) {
			return new ActionNone(comp, childActions, "develop branch is IGNORED");
		}
		
		ReleaseBranch rb = new ReleaseBranch(comp);
		ReleaseBranchStatus rbs = rb.getStatus();

		if (rbs == ReleaseBranchStatus.MISSING) {
			skipAllBuilds(childActions);
			if (actionKind == ActionKind.BUILD) {
				return new ActionNone(comp, childActions, "nothing to build. " + rb.getVersion() + " " + rb.getStatus());
			}
			
			return new SCMActionFork(comp, childActions, ReleaseBranchStatus.MISSING, ReleaseBranchStatus.MDEPS_ACTUAL, options);
		}

		if (rbs == ReleaseBranchStatus.BRANCHED) {
			// need to freeze mdeps
			skipAllBuilds(childActions);
			if (actionKind == ActionKind.BUILD) {
				return new ActionNone(comp, childActions, "nothing to build. " + rb.getVersion() + " " + rb.getStatus());
			}
			return new SCMActionFork(comp, childActions, ReleaseBranchStatus.BRANCHED,  ReleaseBranchStatus.MDEPS_ACTUAL, options);
		}

		if (rbs == ReleaseBranchStatus.MDEPS_FROZEN) {
			if (needToActualizeMDeps(rb)) {
				// need to actualize
				skipAllBuilds(childActions);
				if (actionKind == ActionKind.BUILD) {
					return new ActionNone(comp, childActions, "nothing to build. " + rb.getVersion() + " " + rb.getStatus());
				}
				return new SCMActionFork(comp, childActions, ReleaseBranchStatus.MDEPS_FROZEN, ReleaseBranchStatus.MDEPS_ACTUAL, options);
			} else {
				// All necessary version will be build by Child Actions. Need to build
				skipAllForks(childActions);
				if (actionKind == ActionKind.FORK) {
					return new ActionNone(comp, childActions, "nothing to fork. " + rb.getVersion() + " " + rb.getStatus());
				}
				return new SCMActionBuild(comp, childActions, ReleaseReason.NEW_DEPENDENCIES, rb.getVersion(), options);
			}
		}

		if (rbs == ReleaseBranchStatus.MDEPS_ACTUAL) {
			// need to build
			if (actionKind == ActionKind.FORK) {
				return new ActionNone(comp, childActions, "nothing to fork. " + rb.getVersion() + " " + rb.getStatus());
			}
			return new SCMActionBuild(comp, childActions, ReleaseReason.NEW_FEATURES, rb.getVersion(), options);
		}

		if (hasForkChildActions(childActions)) {
			skipAllBuilds(childActions);
			if (actionKind == ActionKind.FORK) {
				return new ActionNone(comp, childActions, "nothing to build. " + rb.getVersion() + " " + rb.getStatus());
			}
			return new SCMActionFork(comp, childActions, rbs, rbs, options);
		}

		return new ActionNone(comp, childActions, rb.getVersion().toString() + " " + rbs.toString());
	}

	private boolean needToActualizeMDeps(ReleaseBranch currentCompRB) {
		List<Component> mDeps = currentCompRB.getMDeps();
		ReleaseBranch mDepRB;
		for (Component mDep : mDeps) {
			mDepRB = new ReleaseBranch(mDep);
			ReleaseBranchStatus rbs = mDepRB.getStatus();
			if (rbs == ReleaseBranchStatus.MDEPS_ACTUAL) {
				if (!mDepRB.getCurrentVersion().equals(mDep.getVersion())) {
					return true;
				}
			
			} else if (rbs == ReleaseBranchStatus.ACTUAL) {
				if (!mDepRB.getCurrentVersion().toPreviousPatch().equals(mDep.getVersion())) {
					return true;
				}
			} else {
				if (needToActualizeMDeps(mDepRB)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasForkChildActions(List<IAction> childActions) {
		for (IAction action : childActions) {
			if (action instanceof SCMActionFork) {
				return true;
			}
		}
		return false;
	}

	private void skipAllForks(List<IAction> childActions) {
		ListIterator<IAction> li = childActions.listIterator();
		IAction action;
		while (li.hasNext()) {
			action = li.next();
			skipAllForks(action.getChildActions());
			if (action instanceof SCMActionFork) {
				li.set(new ActionNone(((SCMActionFork) action).getComponent(), action.getChildActions(), "fork skipped because not all parent components built"));
			}
		}
	}

	private void skipAllBuilds(List<IAction> childActions) {
		ListIterator<IAction> li = childActions.listIterator();
		IAction action;
		while (li.hasNext()) {
			action = li.next();
			skipAllBuilds(action.getChildActions());
			if (action instanceof SCMActionBuild) {
				li.set(new ActionNone(((SCMActionBuild) action).getComponent(), action.getChildActions(), ((SCMActionBuild) action).getTargetVersion() + 
						" build skipped because not all parent components forked"));
			}
		}
	}
	
	public IAction getTagReleaseAction(Component comp) {
		List<IAction> childActions = new ArrayList<>();
		DevelopBranch db = new DevelopBranch(comp);
		List<Component> mDeps = db.getMDeps();

		for (Component mDep : mDeps) {
			childActions.add(getTagReleaseAction(mDep));
		}
		return getTagReleaseActionRoot(comp, childActions);
	}

	public IAction getTagReleaseAction(String compName) {
		return getTagReleaseAction(new Component(compName));
	}

	private IAction getTagReleaseActionRoot(Component comp, List<IAction> childActions) {
		ReleaseBranch rb = new ReleaseBranch(comp);
		DelayedTagsFile cf = new DelayedTagsFile();
		IVCS vcs = comp.getVCS();
		
		String delayedRevisionToTag = cf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
		
		if (delayedRevisionToTag == null) {
			return new ActionNone(comp, childActions, "no delayed tags");
		}
		
		List<VCSTag> tagsOnRevision = vcs.getTagsOnRevision(delayedRevisionToTag);
		if (tagsOnRevision.isEmpty()) {
			return new SCMActionTagRelease(comp, childActions, options);
		}
		Version delayedTagVersion = new Version(vcs.getFileContent(rb.getName(), SCMReleaser.VER_FILE_NAME, delayedRevisionToTag));
		for (VCSTag tag : tagsOnRevision) {
			if (tag.getTagName().equals(delayedTagVersion.toReleaseString())) {
				return new ActionNone(comp, childActions, "tag " + tag.getTagName() + " already exists");
			}
		}
		return new SCMActionTagRelease(comp, childActions, options);
	}
	
	public static TagDesc getTagDesc(String verStr) {
		String tagName = verStr;
		String tagMessage = tagName + " release";
		return new TagDesc(tagName, tagMessage);
	}

	public List<Option> getOptions() {
		return options;
	}
}
