package org.scm4j.releaser;

import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.scmactions.SCMActionBuild;
import org.scm4j.releaser.scmactions.SCMActionFork;
import org.scm4j.releaser.scmactions.SCMActionTagRelease;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSTag;

import java.io.File;
import java.util.*;

public class SCMReleaser {

	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String DELAYED_TAGS_FILE_NAME = "delayed-tags.yml";
	public static final File BASE_WORKING_DIR = new File(System.getProperty("user.home"), ".scm4j");

	public IAction getActionTree(String coords) {
		return getActionTree(new Component(coords));
	}
	
	public IAction getActionTree(Component comp) {
		return getActionTree(comp, ActionKind.AUTO);
		
	}
	
	public IAction getActionTree(String coords, ActionKind actionKind) {
		Component comp = new Component(coords);
		Options.setIsPatch(comp.getVersion().isExact());
		return getActionTree(new Component(coords), actionKind, new HashMap<Component, CalculatedResult>());
	}
	
	public IAction getActionTree(Component comp, ActionKind actionKind) {
		Options.setIsPatch(comp.getVersion().isExact());
		return getActionTree(comp, actionKind, new HashMap<Component, CalculatedResult>());
	}

	public IAction getActionTree(Component comp, ActionKind actionKind, Map<Component, CalculatedResult> calculatedStatuses) {
		List<IAction> childActions = new ArrayList<>();
		List<Component> mDeps;
		ReleaseBranch rb;
		BuildStatus mbs;
		CalculatedResult cr = calculatedStatuses.get(comp);
		if (cr != null) {
			rb = cr.getReleaseBranch();
			mDeps = cr.getMDeps();
			mbs = cr.getBuildStatus();
		} else {
			if (Options.isPatch()) {
				rb = new ReleaseBranch(comp, comp.getCoords().getVersion());
				mDeps = rb.getMDeps();
			} else {
				rb = new ReleaseBranch(comp);
				mDeps = new DevelopBranch(comp).getMDeps();
			}
			mbs = getBuildStatus(calculatedStatuses, rb);
			calculatedStatuses.put(comp, new CalculatedResult(rb, mbs, mDeps));
		}
		
		for (Component mDep : mDeps) {
			childActions.add(getActionTree(mDep, actionKind, calculatedStatuses));
		}
		
		switch (mbs) {
		case FORK:
		case FREEZE:
			return getForkOrSkipAction(rb, childActions, mbs, actionKind);
		case BUILD_MDEPS:	
		case ACTUALIZE_PATCHES:
		case BUILD:
			return getBuildOrSkipAction(rb, childActions, mbs, actionKind);
		case DONE:
			return new ActionNone(rb, childActions, mbs);
		default:
			throw new IllegalArgumentException("unsupported build status: " + mbs);
		}
	}

	protected BuildStatus getBuildStatus(Map<Component, CalculatedResult> calculatedStatuses, ReleaseBranch rb) {
		BuildStatus mbs;
		Build mb = new Build(rb);
		mbs = mb.getStatus(calculatedStatuses);
		return mbs;
	}

	
	private IAction getBuildOrSkipAction(ReleaseBranch rb, List<IAction> childActions, BuildStatus mbs,
			ActionKind actionKind) {
		if (actionKind == ActionKind.FORK) {
			return new ActionNone(rb, childActions, mbs);
		}
		skipAllForks(childActions);
		return new SCMActionBuild(rb, childActions, mbs);
	}

	private IAction getForkOrSkipAction(ReleaseBranch rb, List<IAction> childActions, BuildStatus mbs,
			ActionKind actionKind) {
		if (actionKind == ActionKind.BUILD) {
			return new ActionNone(rb, childActions, mbs);
		}
		skipAllBuilds(childActions);
		return new SCMActionFork(rb, childActions, mbs);
	}

	public static TagDesc getTagDesc(String verStr) {
		String tagMessage = verStr + " release";
		return new TagDesc(verStr, tagMessage);
	}
	
	private void skipAllForks(List<IAction> childActions) {
		ListIterator<IAction> li = childActions.listIterator();
		IAction action;
		while (li.hasNext()) {
			action = li.next();
			skipAllForks(action.getChildActions());
			if (action instanceof SCMActionFork) {
				li.set(new ActionNone(((SCMActionFork) action).getReleaseBranch(), action.getChildActions(), null, "fork skipped because not all parent components built"));
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
				li.set(new ActionNone(((SCMActionBuild) action).getReleaseBranch(), action.getChildActions(), null, ((SCMActionBuild) action).getVersion() +
						" build skipped because not all parent components forked"));
			}
		}
	}
	
	public IAction getTagActionTree(Component comp) {
		List<IAction> childActions = new ArrayList<>();
		DevelopBranch db = new DevelopBranch(comp);
		List<Component> mDeps = db.getMDeps();

		for (Component mDep : mDeps) {
			childActions.add(getTagActionTree(mDep));
		}
		return getTagActionTree(comp, childActions);
	}

	private IAction getTagActionTree(Component comp, List<IAction> childActions) {
		DelayedTagsFile cf = new DelayedTagsFile();
		IVCS vcs = comp.getVCS();
		
		String delayedRevisionToTag = cf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
		
		if (delayedRevisionToTag == null) {
			return new ActionNone(new ReleaseBranch(comp), childActions, null, "no delayed tags");
		}

		ReleaseBranch rb = new ReleaseBranch(comp);
		List<VCSTag> tagsOnRevision = vcs.getTagsOnRevision(delayedRevisionToTag);
		if (tagsOnRevision.isEmpty()) {
			return new SCMActionTagRelease(new ReleaseBranch(comp), childActions);
		}
		Version delayedTagVersion = new Version(vcs.getFileContent(rb.getName(), SCMReleaser.VER_FILE_NAME, delayedRevisionToTag));
		for (VCSTag tag : tagsOnRevision) {
			if (tag.getTagName().equals(delayedTagVersion.toReleaseString())) {
				return new ActionNone(new ReleaseBranch(comp), childActions, null, "tag " + tag.getTagName() + " already exists");
			}
		}
		return new SCMActionTagRelease(new ReleaseBranch(comp), childActions);
	}
}
