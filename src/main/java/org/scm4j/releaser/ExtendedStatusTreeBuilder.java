package org.scm4j.releaser;

import java.util.LinkedHashMap;
import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.DevelopBranchStatus;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.exceptions.ENoReleaseBranchForPatch;
import org.scm4j.releaser.exceptions.ENoReleases;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;
import org.scm4j.vcs.api.exceptions.EVCSBranchNotFound;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

public class ExtendedStatusTreeBuilder {
	
	private static final int COMMITS_RANGE_LIMIT = 10;
	
	public ExtendedStatusTreeNode getExtendedStatusTreeNode(Component comp, CachedStatuses cache) {
		return getExtendedStatusTreeNode(comp, cache, new ProgressConsole());
	}
	
	public ExtendedStatusTreeNode getExtendedStatusTreeNode(Component comp) {
		return getExtendedStatusTreeNode(comp, new CachedStatuses(), new ProgressConsole());
	}
	
	public ExtendedStatusTreeNode getExtendedStatusTreeNode(Component comp, CachedStatuses cache, IProgress progress) {
		
		ExtendedStatusTreeNode existing = cache.putIfAbsent(comp.getUrl(), ExtendedStatusTreeNode.DUMMY);
		
		if (ExtendedStatusTreeNode.DUMMY == existing) {
			throw new IllegalStateException("Circular dependency detected: %s ");
		}
		
		if(null != existing) {
			return existing;
		}
		
		IVCS vcs = comp.getVCS();
		Version crbVersion = getDevVersion(comp).toPreviousMinor().toReleaseZeroPatch();
		Version latestVersion;
		boolean crbExists;
		try {
			latestVersion = new Version(vcs.getFileContent(Utils.getReleaseBranchName(comp, crbVersion), SCMReleaser.VER_FILE_NAME, null)).toRelease();
			crbExists = true;
		} catch (EVCSBranchNotFound | EVCSFileNotFound e) {
			crbExists = false;
			latestVersion = crbVersion;
		}
		
		List<Component> mdeps;
		if (crbExists && latestVersion.getPatch().equals("0")) {
			mdeps = new ReleaseBranch(comp, latestVersion, crbExists).getMDeps();
		} else {
			mdeps = new DevelopBranch(comp).getMDeps();
		}
		
		// Extended status for subcomponents
		for (Component mdep : mdeps) {
			getExtendedStatusTreeNode(mdep, cache, progress);
		}
		
		LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents = new LinkedHashMap<>();
		for (Component mdep : mdeps) {
			subComponents.put(mdep, cache.get(mdep.getUrl()));
		}
		
		Boolean isNeedToFork = isNeedToFork(comp, latestVersion, crbExists, mdeps, cache);
		
		BuildStatus status = getBuildStatus(comp, isNeedToFork, latestVersion, crbExists, mdeps, cache);
		
		Version targetVersion;
		if (status.ordinal() > BuildStatus.FREEZE.ordinal()) {
			targetVersion = latestVersion;
		} else {
			targetVersion = latestVersion.toNextMinor().toReleaseZeroPatch();
		}
		ExtendedStatusTreeNode res = new ExtendedStatusTreeNode(targetVersion, status, subComponents, comp);
		cache.replace(comp.getUrl(), res);
		return res;

	}

	private BuildStatus getBuildStatus(Component comp, Boolean isNeedToFork, Version latestVersion, boolean crbExists, List<Component> mdeps,
			CachedStatuses cache) {
		if (Options.isPatch()) {
			if (!crbExists) {
				throw new ENoReleaseBranchForPatch("Release Branch does not exists for the requested Component version: " + comp);
			}
			
			if (Integer.parseInt(latestVersion.getPatch()) < 1) {
				throw new ENoReleases("Release Branch version patch is " + latestVersion.getPatch() + ". Component release should be created before patch");
			}
		} else {
			if (!comp.getVersion().isLocked() && isNeedToFork) {
				return BuildStatus.FORK;
			}
			
			if (Integer.parseInt(latestVersion.getPatch()) > 0) {
				return BuildStatus.DONE;
			}
		}
		
		if (!areMDepsFrozen(mdeps)) {
			return BuildStatus.FREEZE;
		}
		
		if (hasMDepsNotInDONEStatus(mdeps, cache)) {
			return BuildStatus.BUILD_MDEPS;
		}

		if (!areMDepsPatchesActual(mdeps, cache)) {
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
			if (!cache.get(mDep.getUrl()).getLatestVersion().equals(mDep.getVersion().toNextPatch())) { 
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

	private Boolean isNeedToFork(Component comp, Version latestVersion, boolean crbExists, List<Component> mdeps, CachedStatuses cache) {
		if (!crbExists) {
			return true;
		}
		
		if (latestVersion.getPatch().equals("0")) {
			return false;
		}
		
		// develop branch has valuable commits => YES
		if (new DevelopBranch(comp).getStatus() == DevelopBranchStatus.MODIFIED) {
			return true;
		}
		
		
		ExtendedStatusTreeNode mdepStatus;
		for (Component mdep : mdeps) {
			mdepStatus = cache.get(mdep.getUrl());
			// any mdeps needs FORK => YES
			if (mdepStatus.getStatus() == BuildStatus.FORK) {
				return true;
			}
			
			// Versions in mdeps does NOT equal to components CR versions => YES
			if (!mdepStatus.getLatestVersion().toPreviousPatch().equals(mdep.getVersion())) {
				return true;
			}
		}
		
		return false;
	}

	public static ReleaseBranch getCRB(Component comp, CalculatedResult calculatedResult, IProgress progress) {
		
		Version candidateVer = getDevVersion(comp).toPreviousMinor().toReleaseZeroPatch();
		Version tempVersion;
		boolean crbExists;
		try {
			IVCS vcs = comp.getVcsRepository().getVcs();
			tempVersion = new Version(vcs.getFileContent(Utils.getReleaseBranchName(comp, candidateVer), SCMReleaser.VER_FILE_NAME, null)).toRelease();
			crbExists = true;
		} catch (EVCSBranchNotFound | EVCSFileNotFound e) {
			crbExists = false;
			tempVersion = candidateVer;
		}
		
		final Version latestVersion = tempVersion;
		
		ReleaseBranch rb = new ReleaseBranch(comp, latestVersion, crbExists);
		if (crbExists && latestVersion.getPatch().equals("0")) {
			calculatedResult.setMDeps(comp, () -> rb.getMDeps(), progress);
		} else {
			calculatedResult.setMDeps(comp, () -> new DevelopBranch(comp).getMDeps(), progress);
		}
		return rb;
	}
	
	public static Version getDevVersion(Component comp) {
		return new Version(comp.getVCS().getFileContent(comp.getVcsRepository().getDevelopBranch(), SCMReleaser.VER_FILE_NAME, null));
	}
}
