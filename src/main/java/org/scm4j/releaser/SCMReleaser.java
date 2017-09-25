package org.scm4j.releaser;

import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.CurrentReleaseBranch;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.scmactions.SCMActionBuild;
import org.scm4j.releaser.scmactions.SCMActionFork;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SCMReleaser {

	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String DELAYED_TAGS_FILE_NAME = "delayed-tags.yml";
	public static final File BASE_WORKING_DIR = new File(System.getProperty("user.home"), ".scm4j");

	public IAction getActionTree(Component comp) {
		return getActionTree(comp, ActionKind.AUTO);
	}

	public IAction getActionTree(String coords) {
		return getActionTree(new Component(coords), ActionKind.AUTO);
	}

	public IAction getActionTree(String coords, ActionKind actionKind) {
		return getActionTree(new Component(coords), actionKind);
	}

	public IAction getActionTree(Component comp, ActionKind actionKind) {
		List<IAction> childActions = new ArrayList<>();
		List<Component> mDeps = new DevelopBranch(comp).getMDeps();

		boolean useSR = comp.getVersion().isExact();
		for (Component mDep : mDeps) {
			childActions.add(getActionTree(useSR ? mDep.toServiceRelease() : mDep, actionKind));
		}

		CurrentReleaseBranch crb = new CurrentReleaseBranch(comp);
		MinorBuild mb = new MinorBuild(crb);
		MinorBuildStatus mbs = mb.getStatus();
		switch (mbs) {
		case FORK:
		case FREEZE:
		case ACTUALIZE_PATCHES:
			return getForkOrSkipAction(crb, childActions, mbs, actionKind);
		case BUILD:
			return getBuildOrSkipAction(crb, childActions, mbs, actionKind);
		case NONE:
			return new ActionNone(crb, childActions, null);
		default:
			throw new IllegalArgumentException("unsupported minor build status: " + mbs);
		}
	}

	private IAction getBuildOrSkipAction(CurrentReleaseBranch crb, List<IAction> childActions, MinorBuildStatus mbs,
			ActionKind actionKind) {
		if (actionKind == ActionKind.FORK) {
			return new ActionNone(crb, childActions, "nothing to build");
		}
		return new SCMActionBuild(crb, childActions, mbs);
	}

	private IAction getForkOrSkipAction(CurrentReleaseBranch crb, List<IAction> childActions, MinorBuildStatus mbs,
			ActionKind actionKind) {
		if (actionKind == ActionKind.BUILD) {
			return new ActionNone(crb, childActions, "nothing to fork");
		}
		return new SCMActionFork(crb, childActions, mbs);
	}

	public static TagDesc getTagDesc(String verStr) {
		String tagMessage = verStr + " release";
		return new TagDesc(verStr, tagMessage);
	}
}
