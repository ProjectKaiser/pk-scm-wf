package org.scm4j.releaser;

import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.DevelopBranchStatus;
import org.scm4j.releaser.branch.MDepsSource;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.Options;
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

		MDepsSource mDepsSource = new MDepsSource(comp);
		
		ExtendedStatusTreeNode res;
		if (Options.isPatch()) {
			res = getPatchTreeNode(comp, mDepsSource, cache, progress);
		} else {
			res = getMinorTreeNode(comp, mDepsSource, cache, progress);
		}
		
		cache.replace(comp.getUrl(), res);
		return res;
	}
	
	private ExtendedStatusTreeNode getMinorTreeNode(Component comp, MDepsSource mDepsSource, CachedStatuses cache,
			IProgress progress) {
		LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents = new LinkedHashMap<>();
		BuildStatus status = getMinorBuildStatus(comp, mDepsSource, cache, progress, subComponents);
		Version nextVersion;
		if (status == BuildStatus.FORK) {
			nextVersion = mDepsSource.getDevVersion().toReleaseZeroPatch();
		} else {
			nextVersion = mDepsSource.getRbVersion();
		}
		return new ExtendedStatusTreeNode(nextVersion, status, subComponents, comp);
	}

	private ExtendedStatusTreeNode getPatchTreeNode(Component comp, MDepsSource mDepsSource, CachedStatuses cache, IProgress progress) {
		LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents = new LinkedHashMap<>();
		BuildStatus status = getPatchBuildStatus(comp, mDepsSource, cache, progress, subComponents);
		Version nextVersion = mDepsSource.getRbVersion();
		return new ExtendedStatusTreeNode(nextVersion, status, subComponents, comp);
	}

	public BuildStatus getPatchBuildStatus(Component comp, MDepsSource mDepsSource, CachedStatuses cache, IProgress progress, LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents) {
		if (!mDepsSource.hasCRB()) {
			throw new ENoReleaseBranchForPatch("Release Branch does not exists for the requested Component version: " + comp);
		}

		// failed if we to build previously forked component which is 0-patched
		if (Options.isPatch() && Integer.parseInt(mDepsSource.getCrbVersion().getPatch()) < 1) {
			throw new ENoReleases("Release Branch version patch is " + mDepsSource.getRbVersion().getPatch() + ". Component release should be created before patch");
		}
		
		if (!areMDepsLocked(mDepsSource.getMDeps())) {
			throw new EReleaserException("not all mdeps locked"); // TODO: add unlocked component output
		}
		
		for (Component mdep : mDepsSource.getMDeps()) {
			ExtendedStatusTreeNode status = getExtendedStatusTreeNode(mdep, cache, progress);
			subComponents.put(mdep, status);
		}

		if (hasMDepsNotInDONEStatus(mDepsSource.getMDeps(), cache)) {
			return BuildStatus.BUILD_MDEPS;
		}

		if (!areMDepsPatchesActual(mDepsSource.getMDeps(), cache)) {
			return BuildStatus.ACTUALIZE_PATCHES;
		}

		if (noValueableCommitsAfterLastTag(comp, mDepsSource)) {
			return BuildStatus.DONE;
		}

		return BuildStatus.BUILD;
	}

	private BuildStatus getMinorBuildStatus(Component comp, MDepsSource mDepsSource, CachedStatuses cache, IProgress progress, LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents) {
	
		if (isNeedToFork(comp, mDepsSource, cache, progress, subComponents)) {
			return BuildStatus.FORK;
		}
		
		if (Integer.parseInt(mDepsSource.getCrbVersion().getPatch()) > 0) {
			return BuildStatus.DONE;
		}

		if (!areMDepsLocked(mDepsSource.getMDeps())) {
			return BuildStatus.LOCK;
		}

		if (hasMDepsNotInDONEStatus(mDepsSource.getMDeps(), cache)) {
			return BuildStatus.BUILD_MDEPS;
		}

		if (!areMDepsPatchesActual(mDepsSource.getMDeps(), cache)) {
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

	private boolean noValueableCommitsAfterLastTag(Component comp, MDepsSource mDepsSource) {
		IVCS vcs = comp.getVCS();
		String startingFromRevision = null;

		DelayedTagsFile dtf = new DelayedTagsFile();
		String delayedTagRevision = dtf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
		List<VCSCommit> commits;
		String branchName;
		if (Options.isPatch()) {
			branchName = mDepsSource.getRbName();
		} else {
			branchName = mDepsSource.getCrbName();
		}
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

	private Boolean isNeedToFork(Component comp, MDepsSource mDepsSource, CachedStatuses cache, IProgress progress, LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents) {
		
		for (Component mdep : mDepsSource.getMDeps()) {
			ExtendedStatusTreeNode mDepStatus = getExtendedStatusTreeNode(mdep, cache, progress);
			subComponents.put(mdep, mDepStatus);
		}
		
		if (!mDepsSource.hasCRB()) {
			return true;
		} 

		if (mDepsSource.getCrbVersion().getPatch().equals(Utils.ZERO_PATCH)) {
			return false;
		}
		
		// develop branch has valuable commits => YES
		if (DevelopBranchStatus.MODIFIED == new DevelopBranch(comp).getStatus()) {
			return true;
		}
		
		ExtendedStatusTreeNode mdepStatus;
		for (Component mdep : mDepsSource.getCRBMDeps()) {
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
