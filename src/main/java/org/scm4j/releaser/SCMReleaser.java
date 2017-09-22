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
		return getProductionReleaseActionFiltered(new Component(componentName, true), ActionKind.AUTO);
	}
	
	public IAction getProductionReleaseAction(String componentCoords, ActionKind actionKind) {
		return getProductionReleaseActionFiltered(new Component(componentCoords, true), actionKind);
	}
	
	public IAction getProductionReleaseAction(Component comp) {
		return getProductionReleaseActionFiltered(comp, ActionKind.AUTO);
	}
	
	private IAction getProductionReleaseActionFiltered(Component comp, ActionKind actionKind) {
		IAction res = getProductionReleaseActionUnfiltered(comp);
		filterUnsuitableActions(res, actionKind);
		filterChildsByFirstParentActionType(res, null);
		return res;
	}
	
	private void filterChildsByFirstParentActionType(IAction res, Class<?> firstParentActionClass) {
		ListIterator<IAction> li = res.getChildActions().listIterator();
		IAction action;
		if (firstParentActionClass == null) {
			if (res instanceof SCMActionBuild) {
				firstParentActionClass = SCMActionBuild.class;
			} else if (res instanceof SCMActionFork) {
				firstParentActionClass = SCMActionFork.class;
			}
		}
		while (li.hasNext()) {
			action = li.next();
			filterChildsByFirstParentActionType(action, firstParentActionClass);
			if (firstParentActionClass == SCMActionFork.class) {
				if (action instanceof SCMActionBuild) {
					li.set(new ActionNone(((SCMActionBuild) action).getComponent(), action.getChildActions(), ((SCMActionBuild) action).getTargetVersion() + 
							" build skipped because not all parent components forked"));
				}
			}
			if (firstParentActionClass == SCMActionBuild.class) {
				if (action instanceof SCMActionFork) {
					li.set(new ActionNone(((SCMActionFork) action).getComponent(), action.getChildActions(), "fork skipped because not all parent components built"));
				}
			}
		}
	}

	private void filterUnsuitableActions(IAction res, ActionKind actionKind) {
		if (actionKind == ActionKind.AUTO) {
			return;
		}
		ListIterator<IAction> li = res.getChildActions().listIterator();
		IAction action;
		while (li.hasNext()) {
			action = li.next();
			filterUnsuitableActions(action, actionKind);
			switch (actionKind) {
			case FORK:
				if (action instanceof SCMActionBuild) {
					li.set(new ActionNone(((SCMActionBuild) action).getComponent(), action.getChildActions(), ((SCMActionBuild) action).getTargetVersion() + 
							" build skipped because not all parent components forked"));
				}
				break;
			case BUILD:
				if (action instanceof SCMActionFork) {
					li.set(new ActionNone(((SCMActionFork) action).getComponent(), action.getChildActions(), "fork skipped because not all parent components built"));
				}
				break;
			default: {
				throw new IllegalArgumentException("Unsupported action kind: " + actionKind);
			}
			}
		}
	}

	private IAction getProductionReleaseActionUnfiltered(Component comp) {
		List<IAction> childActions = new ArrayList<>();
		
		/**
		 * all components are forked and ready o build. But Product is not changed. We have rb pointing to previous release.
		 * rb exists, so mdeps list will contain exact versions (for prev Product release) . So new versions of mdpes will not be taken. So need to use dev mdeps (snapshots) always. 
		 */
		List<Component> mDeps = new DevelopBranch(comp).getMDeps();

		boolean useSR = comp.getVersion().isExact();
		for (Component mDep : mDeps) {
			childActions.add(getProductionReleaseActionUnfiltered(useSR ? mDep.toServiceRelease() : mDep)); 
		}

		ReleaseBranch rb = new ReleaseBranch(comp);
		return getProductionReleaseActionRoot(comp, rb, childActions);
	}
		
	private  IAction getProductionReleaseActionRoot(Component comp, ReleaseBranch rb, List<IAction> childActions) {
		DevelopBranch db = new DevelopBranch(comp);
		if (!db.hasVersionFile()) {
			throw new EComponentConfig("no " + VER_FILE_NAME + " file for " + comp.toString());
		}
		DevelopBranchStatus dbs = db.getStatus();

		if (dbs == DevelopBranchStatus.IGNORED) {
			return new ActionNone(comp, childActions, "develop branch is IGNORED");
		}
		
		ReleaseBranchStatus rbs = rb.getStatus();

		if (rbs == ReleaseBranchStatus.MISSING) {
			return new SCMActionFork(comp, rb, childActions, ReleaseBranchStatus.MISSING, ReleaseBranchStatus.MDEPS_ACTUAL, options);
		}

		if (rbs == ReleaseBranchStatus.BRANCHED) {
			// need to freeze mdeps
			return new SCMActionFork(comp, rb, childActions, ReleaseBranchStatus.BRANCHED,  ReleaseBranchStatus.MDEPS_ACTUAL, options);
		}

		if (rbs == ReleaseBranchStatus.MDEPS_FROZEN) {
			if (needToActualizeMDeps(rb)) {
				// need to actualize
				return new SCMActionFork(comp, rb, childActions, ReleaseBranchStatus.MDEPS_FROZEN, ReleaseBranchStatus.MDEPS_ACTUAL, options);
			} else {
				// All necessary version will be build by Child Actions. Need to build
				return new SCMActionBuild(comp, rb, childActions, ReleaseReason.NEW_DEPENDENCIES, rb.getVersion(), options);
			}
		}

		if (rbs == ReleaseBranchStatus.MDEPS_ACTUAL) {
			// need to build
			return new SCMActionBuild(comp, rb, childActions, ReleaseReason.NEW_FEATURES, rb.getVersion(), options);
		}

		// TODO: add test: product is ACTUAL, but child was forked sepearately. Product should be forked because component needs to be built.
		if (hasSignificantActions(childActions)) {
			// we are ACTUAL and have child forks or builds => we need to be forked
			return new SCMActionFork(comp, new ReleaseBranch(comp, db.getVersion()), childActions, ReleaseBranchStatus.MISSING, ReleaseBranchStatus.MDEPS_ACTUAL, options);
		}

		return new ActionNone(comp, childActions, getReleaseBranchDetailsStr(rb, rbs));
	}

	private String getReleaseBranchDetailsStr(ReleaseBranch rb, ReleaseBranchStatus rbs) {
		return rb.getName() + " " + rbs + ", target version " + rb.getVersion();
	}

	private boolean needToActualizeMDeps(ReleaseBranch currentCompRB) {
		List<Component> mDeps = currentCompRB.getMDeps();
		ReleaseBranch mDepRB;
		for (Component mDep : mDeps) {
			mDepRB = new ReleaseBranch(mDep);
			ReleaseBranchStatus rbs = mDepRB.getStatus();
			if (rbs == ReleaseBranchStatus.MDEPS_ACTUAL) {
				if (!mDepRB.getHeadVersion().equals(mDep.getVersion())) {
					return true;
				}
			
			} else if (rbs == ReleaseBranchStatus.ACTUAL) {
				if (!mDepRB.getHeadVersion().toPreviousPatch().equals(mDep.getVersion())) {
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

	private boolean hasSignificantActions(List<IAction> childActions) {
		for (IAction action : childActions) {
			if (action instanceof SCMActionFork || action instanceof SCMActionBuild) {
				return true;
			}
		}
		return false;
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
		DelayedTagsFile cf = new DelayedTagsFile();
		IVCS vcs = comp.getVCS();
		
		String delayedRevisionToTag = cf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
		
		if (delayedRevisionToTag == null) {
			return new ActionNone(comp, childActions, "no delayed tags");
		}

		ReleaseBranch rb = new ReleaseBranch(comp);
		List<VCSTag> tagsOnRevision = vcs.getTagsOnRevision(delayedRevisionToTag);
		if (tagsOnRevision.isEmpty()) {
			return new SCMActionTagRelease(comp, rb, childActions, options);
		}
		Version delayedTagVersion = new Version(vcs.getFileContent(rb.getName(), SCMReleaser.VER_FILE_NAME, delayedRevisionToTag));
		for (VCSTag tag : tagsOnRevision) {
			if (tag.getTagName().equals(delayedTagVersion.toReleaseString())) {
				return new ActionNone(comp, childActions, "tag " + tag.getTagName() + " already exists");
			}
		}
		return new SCMActionTagRelease(comp, rb, childActions, options);
	}
	
	public static TagDesc getTagDesc(String verStr) {
		String tagMessage = verStr + " release";
		return new TagDesc(verStr, tagMessage);
	}

	public List<Option> getOptions() {
		return options;
	}
}
