package org.scm4j.releaser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.scm4j.commons.Coords;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.scmactions.SCMActionBuild;
import org.scm4j.releaser.scmactions.SCMActionFork;

public class SCMReleaser {

	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String DELAYED_TAGS_FILE_NAME = "delayed-tags.yml";
	public static final File BASE_WORKING_DIR = new File(System.getProperty("user.home"), ".scm4j");

	public IAction getActionTree(Component comp) {
		return getActionTree(comp, ActionKind.AUTO, comp.getVersion().isExact());
	}

	public IAction getActionTree(String coords) {
		return getActionTree(new Component(coords));
	}

	public IAction getActionTree(String coords, ActionKind actionKind) {
		return getActionTree(new Component(coords), actionKind, new Coords(coords).getVersion().isExact());
	}

	public IAction getActionTree(Component comp, ActionKind actionKind, boolean isPatch) {
		List<IAction> childActions = new ArrayList<>();
		List<Component> mDeps;
		ReleaseBranch crb;
		if (isPatch) {
			crb = new ReleaseBranch(comp, comp.getCoords().getVersion());
			mDeps = crb.getMDeps();
		} else {
			crb = new ReleaseBranch(comp);
			mDeps = new DevelopBranch(comp).getMDeps();
		}

		for (Component mDep : mDeps) {
			childActions.add(getActionTree(mDep, actionKind, isPatch));
		}

		Build mb = new Build(crb);
		BuildStatus mbs = mb.getStatus();
		switch (mbs) {
		case FORK:
		case FREEZE:
		case ACTUALIZE_PATCHES:
			return getForkOrSkipAction(crb, childActions, mbs, actionKind);
		case BUILD:
			return getBuildOrSkipAction(crb, childActions, mbs, actionKind);
		case NONE:
			return new ActionNone(crb, childActions, null);
		case IGNORED:
			return new ActionNone(crb, childActions, "develop branch is IGONRED");
		default:
			throw new IllegalArgumentException("unsupported minor build status: " + mbs);
		}
	}

	private IAction getBuildOrSkipAction(ReleaseBranch crb, List<IAction> childActions, BuildStatus mbs,
			ActionKind actionKind) {
		if (actionKind == ActionKind.FORK) {
			return new ActionNone(crb, childActions, "nothing to build");
		}
		skipAllForks(crb, childActions);
		return new SCMActionBuild(crb, childActions, mbs);
	}

	private IAction getForkOrSkipAction(ReleaseBranch crb, List<IAction> childActions, BuildStatus mbs,
			ActionKind actionKind) {
		if (actionKind == ActionKind.BUILD) {
			return new ActionNone(crb, childActions, "nothing to fork");
		}
		skipAllBuilds(crb, childActions);
		return new SCMActionFork(crb, childActions, mbs);
	}

	public static TagDesc getTagDesc(String verStr) {
		String tagMessage = verStr + " release";
		return new TagDesc(verStr, tagMessage);
	}
	
	private void skipAllForks(ReleaseBranch crb, List<IAction> childActions) {
		ListIterator<IAction> li = childActions.listIterator();
		IAction action;
		while (li.hasNext()) {
			action = li.next();
			skipAllForks(crb, action.getChildActions());
			if (action instanceof SCMActionFork) {
				li.set(new ActionNone(crb, action.getChildActions(), "fork skipped because not all parent components built"));
			}
		}
	}

	private void skipAllBuilds(ReleaseBranch crb, List<IAction> childActions) {
		ListIterator<IAction> li = childActions.listIterator();
		IAction action;
		while (li.hasNext()) {
			action = li.next();
			skipAllBuilds(crb, action.getChildActions());
			if (action instanceof SCMActionBuild) {
				li.set(new ActionNone(crb, action.getChildActions(), ((SCMActionBuild) action).getTargetVersion() + 
						" build skipped because not all parent components forked"));
			}
		}
	}
}
