package org.scm4j.releaser;

import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.branch.ReleaseBranchBuilder;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.exceptions.ENoReleaseBranchForPatch;
import org.scm4j.releaser.exceptions.ENoReleases;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

public class ExtendedStatusTreeBuilder {

	private static final int COMMITS_RANGE_LIMIT = 10;
	
	public ExtendedStatusTreeNode getExtendedStatusTreeNode(Component comp) {
		CachedStatuses cache = new CachedStatuses();
		return getExtendedStatusTreeNode(comp, cache);
	}

	public ExtendedStatusTreeNode getExtendedStatusTreeNode(Component comp, CachedStatuses cache) {
		return getExtendedStatusTreeNode(comp, cache, new ProgressConsole(), false);
	}
	
	public ExtendedStatusTreeNode getExtendedStatusTreeNodeForPatch(Component comp, CachedStatuses cache) {
		return getExtendedStatusTreeNode(comp, cache, new ProgressConsole(), true);
	}

	public ExtendedStatusTreeNode getExtendedStatusTreeNode(Component comp, CachedStatuses cache, IProgress progress, boolean isForPatch) {
		ExtendedStatusTreeNode existing = cache.putIfAbsent(comp.getUrl(), ExtendedStatusTreeNode.DUMMY);

		if (ExtendedStatusTreeNode.DUMMY == existing) {
			throw new NotImplementedException(comp + " status is calculating already elsewhere");
		}

		if (null != existing) {
			return existing;
		}

		ReleaseBranchBuilder rbb = new ReleaseBranchBuilder();
		ExtendedStatusTreeNode res;
		if (isForPatch) {
			res = getPatchTreeNode(comp, rbb.getReleaseBranchPatch(comp), cache, progress);
		} else {
			res = getMinorTreeNode(comp, rbb.getReleaseBranchCRB(comp), cache, progress);
		}
		
		cache.replace(comp.getUrl(), res);
		return res;
	}
	
	private ExtendedStatusTreeNode getMinorTreeNode(Component comp, ReleaseBranch rb, CachedStatuses cache,
			IProgress progress) {
		LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents = new LinkedHashMap<>();
		BuildStatus status = getMinorBuildStatus(comp, rb, cache, progress, subComponents);
		Version nextVersion;
		if (status == BuildStatus.FORK) {
			nextVersion = new DevelopBranch(comp).getVersion().toReleaseZeroPatch();
		} else {
			nextVersion = rb.getVersion();
		}
		return new ExtendedStatusTreeNode(nextVersion, status, subComponents, comp);
	}

	private ExtendedStatusTreeNode getPatchTreeNode(Component comp, ReleaseBranch rb, CachedStatuses cache, IProgress progress) {
		LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents = new LinkedHashMap<>();
		BuildStatus status = getPatchBuildStatus(comp, rb, cache, progress, subComponents);
		Version nextVersion = rb.getVersion();
		return new ExtendedStatusTreeNode(nextVersion, status, subComponents, comp);
	}

	public BuildStatus getPatchBuildStatus(Component comp, ReleaseBranch rb, CachedStatuses cache, IProgress progress, LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents) {
		if (!rb.exists()) {
			throw new ENoReleaseBranchForPatch("Release Branch does not exists for the requested Component version: " + comp);
		}

		if (Integer.parseInt(rb.getVersion().getPatch()) < 1) {
			throw new ENoReleases("Release Branch version patch is " + rb.getVersion().getPatch() + ". Component release should be created before patch");
		}
		
		if (!areMDepsLocked(rb.getMDeps())) {
			throw new EReleaserException("not all mdeps locked"); // TODO: add unlocked component output
		}
		
		for (Component mdep : rb.getMDeps()) {
			ExtendedStatusTreeNode status = getExtendedStatusTreeNode(mdep, cache, progress, true);
			subComponents.put(mdep, status);
		}

		if (hasMDepsNotInDONEStatus(rb.getMDeps(), cache)) {
			return BuildStatus.BUILD_MDEPS;
		}

		if (!areMDepsPatchesActual(rb.getMDeps(), cache)) {
			return BuildStatus.ACTUALIZE_PATCHES;
		}

		if (noValueableCommitsAfterLastTag(comp, rb)) {
			return BuildStatus.DONE;
		}

		return BuildStatus.BUILD;
	}

	private BuildStatus getMinorBuildStatus(Component comp, ReleaseBranch rb, CachedStatuses cache, IProgress progress, LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents) {
	
		if (isNeedToFork(comp, rb, cache, progress, subComponents)) {
			return BuildStatus.FORK;
		}
		
		if (Integer.parseInt(rb.getVersion().getPatch()) > 0) {
			return BuildStatus.DONE;
		}

		if (!areMDepsLocked(rb.getMDeps())) {
			return BuildStatus.LOCK;
		}

		if (hasMDepsNotInDONEStatus(rb.getMDeps(), cache)) {
			return BuildStatus.BUILD_MDEPS;
		}

		if (!areMDepsPatchesActual(rb.getMDeps(), cache)) {
			return BuildStatus.ACTUALIZE_PATCHES;
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

	private boolean noValueableCommitsAfterLastTag(Component comp, ReleaseBranch rb) {
		IVCS vcs = comp.getVCS();
		String startingFromRevision = null;

		DelayedTagsFile dtf = new DelayedTagsFile();
		String delayedTagRevision = dtf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
		List<VCSCommit> commits;
		String branchName;
		branchName = rb.getName();
		do {
			commits = vcs.getCommitsRange(branchName, startingFromRevision, WalkDirection.DESC, COMMITS_RANGE_LIMIT);
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
			if (!cache.get(mDep.getUrl()).getNextVersion().equals(mDep.getVersion().toNextPatch())) { //mDep.getVersion().toNextPatch()
				return false;
			}
		}
		return true;
	}

	private boolean areMDepsLocked(List<Component> mDeps) {
		for (Component mDep : mDeps) {
			if (mDep.getVersion().isSnapshot()) {
				return false;
			}
		}
		return true;
	}

	private Boolean isNeedToFork(Component comp, ReleaseBranch rb, CachedStatuses cache, IProgress progress, LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents) {
		
		for (Component mdep : rb.getMDeps()) {
			ExtendedStatusTreeNode mDepStatus = getExtendedStatusTreeNode(mdep, cache, progress, false);
			subComponents.put(mdep, mDepStatus);
		}
		
		if (!rb.exists()) {
			return true;
		} 

		if (rb.getVersion().getPatch().equals(Utils.ZERO_PATCH)) {
			return false;
		}
		
		// develop branch has valuable commits => YES
		if (new DevelopBranch(comp).isModified()) {
			return true;
		}
		
		ExtendedStatusTreeNode mdepStatus;
		for (Component mdep : rb.getCRBDeps()) {
			mdepStatus = cache.get(mdep.getUrl());
			// any mdeps needs FORK => YES
			if (mdepStatus.getStatus() != BuildStatus.DONE) {
				return true;
			}

			// Versions in mdeps does NOT equal to components CR versions => YES
			if (!mdep.getVersion().toReleaseZeroPatch().equals(mdepStatus.getNextVersion().toReleaseZeroPatch())) {
				return true;
			}
		}

		return false;
	}

	
}
