package org.scm4j.releaser;

import org.scm4j.commons.Coords;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.scmactions.SCMActionBuild;
import org.scm4j.releaser.scmactions.SCMActionFork;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
		ReleaseBranch rb;
		if (isPatch) {
			rb = new ReleaseBranch(comp, comp.getCoords().getVersion());
			mDeps = rb.getMDeps();
		} else {
			rb = new ReleaseBranch(comp);
			mDeps = new DevelopBranch(comp).getMDeps();
		}

		for (Component mDep : mDeps) {
			childActions.add(getActionTree(mDep, actionKind, isPatch));
		}

		Build mb = new Build(rb);
		BuildStatus mbs = mb.getStatus();
		switch (mbs) {
		case FORK:
		case FREEZE:
		case ACTUALIZE_PATCHES:
			return getForkOrSkipAction(rb, childActions, mbs, actionKind);
		case BUILD:
			return getBuildOrSkipAction(rb, childActions, mbs, actionKind);
		case NONE:
			return new ActionNone(rb, childActions, null);
		case IGNORED:
			return new ActionNone(rb, childActions, "develop branch is IGONRED");
		default:
			throw new IllegalArgumentException("unsupported minor build status: " + mbs);
		}
	}

	private IAction getBuildOrSkipAction(ReleaseBranch rb, List<IAction> childActions, BuildStatus mbs,
			ActionKind actionKind) {
		if (actionKind == ActionKind.FORK) {
			return new ActionNone(rb, childActions, "nothing to build");
		}
		skipAllForks(rb, childActions);
		return new SCMActionBuild(rb, childActions, mbs);
	}

	private IAction getForkOrSkipAction(ReleaseBranch rb, List<IAction> childActions, BuildStatus mbs,
			ActionKind actionKind) {
		if (actionKind == ActionKind.BUILD) {
			return new ActionNone(rb, childActions, "nothing to fork");
		}
		skipAllBuilds(rb, childActions);
		return new SCMActionFork(rb, childActions, mbs);
	}

	public static TagDesc getTagDesc(String verStr) {
		String tagMessage = verStr + " release";
		return new TagDesc(verStr, tagMessage);
	}
	
	private void skipAllForks(ReleaseBranch rb, List<IAction> childActions) {
		ListIterator<IAction> li = childActions.listIterator();
		IAction action;
		while (li.hasNext()) {
			action = li.next();
			skipAllForks(rb, action.getChildActions());
			if (action instanceof SCMActionFork) {
				li.set(new ActionNone(rb, action.getChildActions(), "fork skipped because not all parent components built"));
			}
		}
	}

	private void skipAllBuilds(ReleaseBranch rb, List<IAction> childActions) {
		ListIterator<IAction> li = childActions.listIterator();
		IAction action;
		while (li.hasNext()) {
			action = li.next();
			skipAllBuilds(rb, action.getChildActions());
			if (action instanceof SCMActionBuild) {
				li.set(new ActionNone(rb, action.getChildActions(), ((SCMActionBuild) action).getVersion() +
						" build skipped because not all parent components forked"));
			}
		}
	}
}
