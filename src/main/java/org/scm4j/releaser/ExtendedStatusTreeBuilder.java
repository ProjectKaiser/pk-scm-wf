package org.scm4j.releaser;

import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.DevelopBranchStatus;
import org.scm4j.releaser.branch.WorkingBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.exceptions.ENoReleaseBranchForPatch;
import org.scm4j.releaser.exceptions.ENoReleases;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

public class ExtendedStatusTreeBuilder {

	private static final int COMMITS_RANGE_LIMIT = 10;

	public ExtendedStatusTreeNode getExtendedStatusTreeNode(Component comp, CachedStatuses cache) {
		return getExtendedStatusTreeNode(comp, cache, new ProgressConsole());
	}

	public ExtendedStatusTreeNode getExtendedStatusTreeNode(Component comp, CachedStatuses cache, IProgress progress) {

		ExtendedStatusTreeNode existing = cache.putIfAbsent(comp.getUrl(), ExtendedStatusTreeNode.DUMMY);

		if (ExtendedStatusTreeNode.DUMMY == existing) {
			throw new NotImplementedException(comp + " status is calculating already elsewhere");
		}

		if (null != existing) {
			return existing;
		}

		WorkingBranch wb = new WorkingBranch(comp);
		
		for (Component mdep : wb.getMdeps()) {
			cache.put(mdep.getUrl(), getExtendedStatusTreeNode(comp, cache, progress));
		}
		
		BuildStatus status;
		if (Options.isPatch()) {
			status = getMinorBuildStatus(comp, wb, cache);
		} else {
			status = getPatchBuildStatus(comp, wb, cache);
		}
		
		LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents = new LinkedHashMap<>();
		for (Component mdep : wb.getMdeps()) {
			subComponents.put(mdep, cache.get(mdep.getUrl()));
		}

		ExtendedStatusTreeNode res = new ExtendedStatusTreeNode(wb.getVersion(), status, subComponents, comp);
		cache.replace(comp.getUrl(), res);
		return res;
	}

	private BuildStatus getPatchBuildStatus(Component comp, WorkingBranch wb, CachedStatuses cache) {
		if (!wb.isDevelop()) {
			throw new ENoReleaseBranchForPatch("Release Branch does not exists for the requested Component version: " + comp);
		}

		if (Integer.parseInt(wb.getVersion().getPatch()) < 1) {
			throw new ENoReleases("Release Branch version patch is " + wb.getVersion().getPatch() + ". Component release should be created before patch");
		}

		if (!areMDepsFrozen(wb.getMdeps())) {
			return BuildStatus.LOCK;
		}

		if (hasMDepsNotInDONEStatus(wb.getMdeps(), cache)) {
			return BuildStatus.BUILD_MDEPS;
		}

		if (!areMDepsPatchesActual(wb.getMdeps(), cache)) {
			return BuildStatus.ACTUALIZE_PATCHES;
		}

		if (Options.isPatch() && noValueableCommitsAfterLastTag(comp)) {
			return BuildStatus.DONE;
		}

		return BuildStatus.BUILD;
	}

	private BuildStatus getMinorBuildStatus(Component comp, WorkingBranch wb, CachedStatuses cache) {
		if (!comp.getVersion().isLocked() && isNeedToFork(comp, wb, cache)) {
			return BuildStatus.FORK;
		}
		
		if (Integer.parseInt(wb.getVersion().getPatch()) > 0) {
			return BuildStatus.DONE;
		}

		if (!areMDepsFrozen(wb.getMdeps())) {
			return BuildStatus.LOCK;
		}

		if (hasMDepsNotInDONEStatus(wb.getMdeps(), cache)) {
			return BuildStatus.BUILD_MDEPS;
		}

		if (!areMDepsPatchesActual(wb.getMdeps(), cache)) {
			return BuildStatus.ACTUALIZE_PATCHES;
		}

		if (Options.isPatch() && noValueableCommitsAfterLastTag(comp)) {
			return BuildStatus.DONE;
		}

		return BuildStatus.BUILD;


	}

	private boolean hasMDepsNotInDONEStatus(List<Component> mDeps, CachedStatuses cache) {
		for (Component mDep : mDeps) {
			if (cache.get(mDep.getUrl()).getStatus() != BuildStatus.DONE) {
				return true;
			}
		}
		return false;
	}

	private boolean noValueableCommitsAfterLastTag(Component comp) {
		IVCS vcs = comp.getVCS();
		String startingFromRevision = null;

		DelayedTagsFile dtf = new DelayedTagsFile();
		String delayedTagRevision = dtf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
		List<VCSCommit> commits;
		String crbName = Utils.getReleaseBranchName(comp, comp.getVersion());
		do {
			commits = vcs.getCommitsRange(crbName, startingFromRevision, WalkDirection.DESC, COMMITS_RANGE_LIMIT);
			for (VCSCommit commit : commits) {
				if (commit.getRevision().equals(delayedTagRevision)) {
					return true;
				}
				List<VCSTag> tags = vcs.getTagsOnRevision(commit.getRevision());
				if (!commit.getLogMessage().contains(LogTag.SCM_VER) && !commit.getLogMessage().contains(LogTag.SCM_IGNORE)) {
					return !tags.isEmpty();
				}
				if (!tags.isEmpty()) {
					return true;
				}
				startingFromRevision = commit.getRevision();
			}
		} while (commits.size() >= COMMITS_RANGE_LIMIT);
		return true;
	}

	private boolean areMDepsPatchesActual(List<Component> mDeps, CachedStatuses cache) {
		for (Component mDep : mDeps) {
			if (!cache.get(mDep.getUrl()).getWBVersion().equals(mDep.getVersion().toNextPatch())) {
				return false;
			}
		}
		return true;
	}

	private boolean areMDepsFrozen(List<Component> mDeps) {
		for (Component mDep : mDeps) {
			if (mDep.getVersion().isSnapshot()) {
				return false;
			}
		}
		return true;
	}

	private Boolean isNeedToFork(Component comp, WorkingBranch wb, CachedStatuses cache) {
		if (wb.isDevelop()) {
			return true;
		}

		if (wb.getVersion().getPatch().equals("0")) {
			return false;
		}
		
		// develop branch has valuable commits => YES
		if (DevelopBranchStatus.MODIFIED == new DevelopBranch(comp).getStatus()) {
			return true;
		}
		
		ExtendedStatusTreeNode mdepStatus;
		for (Component mdep : wb.getMdeps()) {
			mdepStatus = cache.get(mdep.getUrl());
			// any mdeps needs FORK => YES
			if (mdepStatus.getStatus() == BuildStatus.FORK) {
				return true;
			}

			// Versions in mdeps does NOT equal to components CR versions => YES
			if (!mdepStatus.getWBVersion().toPreviousPatch().equals(mdep.getVersion())) {
				return true;
			}
		}

		return false;
	}
}
