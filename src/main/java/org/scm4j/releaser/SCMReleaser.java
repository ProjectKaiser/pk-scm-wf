package org.scm4j.releaser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
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

public class SCMReleaser {

	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String DELAYED_TAGS_FILE_NAME = "delayed-tags.yml";
	public static final File BASE_WORKING_DIR = new File(System.getProperty("user.home"), ".scm4j");

	public IAction getActionTree(String coords) throws Exception {
		return getActionTree(new Component(coords));
	}
	
	public IAction getActionTree(Component comp) throws Exception {
		return getActionTree(comp, ActionKind.ALL);
		
	}
	
	public IAction getActionTree(String coords, ActionKind actionKind) throws Exception {
		Component comp = new Component(coords);
		Options.setIsPatch(comp.getVersion().isExact());
		return getActionTree(new Component(coords), actionKind, new HashMap<Component, CalculatedResult>());
	}
	
	public IAction getActionTree(Component comp, ActionKind actionKind) throws Exception {
		Options.setIsPatch(comp.getVersion().isExact());
		return getActionTree(comp, actionKind, new HashMap<Component, CalculatedResult>());
	}

	public IAction getActionTree(Component comp, ActionKind actionKind, Map<Component, CalculatedResult> calculatedStatuses) throws Exception {
		List<IAction> childActions = new ArrayList<>();
		IProgress progress = new ProgressConsole();
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
				mbs = getBuildStatus(rb);
			} else {
				// If we are build, build_mdeps or actualize_patches then we need to use mdeps from release branches to show what versions we are going to build or actualize
				progress.startTrace("determining release branch version for " + comp.getCoordsNoComment() + "...");
				rb = new ReleaseBranch(comp);
				progress.endTrace("done");
				mbs = getBuildStatus(rb);
				if (mbs == BuildStatus.FORK) {
					// untill has untilldb, ubl has untilldb. untill is BUILD_MDEPS, UBL has release branch but need to FORK. 
					// result: regressinon for untill FORK, regiression for UBL is DONE prev version (mdep fro existing UBL RB is used) 
					// TODO: add test: untill build_mdeps, untill needs to be forked. UBL has release rbanch but has to be forked also. untilldbs must have the same status
					progress.startTrace("reading mdeps from dev branch...");
					mDeps = new DevelopBranch(comp).getMDeps();
					progress.endTrace("done");
				} else {
					if (rb.exists()) {
						progress.startTrace("reading mdeps from release branch " + rb.getName() + "...");
						mDeps = rb.getMDeps();
						progress.endTrace("done");
					} else {
						progress.startTrace("reading mdeps from dev branch");
						mDeps = new DevelopBranch(comp).getMDeps();
						progress.endTrace("done");
					}
				}
			}
			calculatedStatuses.put(comp, new CalculatedResult(rb, mbs, mDeps));
		}
		
		for (Component mDep : mDeps) {
			childActions.add(getActionTree(mDep, actionKind, calculatedStatuses));
		}
		
		progress.close();
		
		switch (mbs) {
		case FORK:
		case FREEZE:
			return getForkAction(rb, childActions, mbs, actionKind);
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

	protected BuildStatus getBuildStatus(ReleaseBranch rb) throws Exception {
		Build mb = new Build(rb);
		IProgress progress = new ProgressConsole();
		progress.startTrace("calculating status for " + rb.getComponent().getName() + "...");
		BuildStatus mbs = mb.getStatus();
		progress.endTrace("done");
		progress.close();
		return mbs;
	}
	
	private IAction getBuildOrSkipAction(ReleaseBranch rb, List<IAction> childActions, BuildStatus mbs,
			ActionKind actionKind) {
		if (actionKind == ActionKind.FORK) {
			return new ActionNone(rb, childActions, mbs);
		}
		// seems we never have situation when root is going to build and child is going to fork.
		return new SCMActionBuild(rb, childActions, mbs);
	}

	private IAction getForkAction(ReleaseBranch rb, List<IAction> childActions, BuildStatus mbs,
			ActionKind actionKind) {
		skipAllBuilds(childActions);
		return new SCMActionFork(rb, childActions, mbs);
	}

	public static TagDesc getTagDesc(String verStr) {
		String tagMessage = verStr + " release";
		return new TagDesc(verStr, tagMessage);
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
