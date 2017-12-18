package org.scm4j.releaser;

import static org.scm4j.releaser.Utils.reportDuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranchCurrent;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.branch.ReleaseBranchPatch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.exceptions.ENoReleaseBranchForPatch;
import org.scm4j.releaser.exceptions.ENoReleases;
import org.scm4j.releaser.exceptions.EReleaseMDepsNotLocked;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

public class ExtendedStatusBuilder {

	private static final int PARALLEL_CALCULATION_AWAIT_TIME = 500;
	private static final int COMMITS_RANGE_LIMIT = 10;
	
	public ExtendedStatus getAndCacheMinorStatus(Component comp) {
		CachedStatuses cache = new CachedStatuses();
		return getAndCacheMinorStatus(comp, cache);
	}

	public ExtendedStatus getAndCacheMinorStatus(Component comp, CachedStatuses cache) {
		return getAndCacheStatus(comp, cache, new ProgressConsole(), false);
	}
	
	public ExtendedStatus getAndCachePatchStatus(Component comp, CachedStatuses cache) {
		return getAndCacheStatus(comp, cache, new ProgressConsole(), true);
	}

	public ExtendedStatus getAndCacheStatus(Component comp, CachedStatuses cache, IProgress progress, boolean patch) {
		ExtendedStatus existing = cache.putIfAbsent(comp.getUrl(), ExtendedStatus.DUMMY);
		
		while (ExtendedStatus.DUMMY == existing) {
			try {
				Thread.sleep(PARALLEL_CALCULATION_AWAIT_TIME);
				existing = cache.get(comp.getUrl());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		if (null != existing) {
			return new ExtendedStatus(existing.getNextVersion(), existing.getStatus(), existing.getSubComponents(), comp);
		}

		ExtendedStatus res = patch ? 
			getPatchStatus(comp, cache, progress) :
			getMinorStatus(comp, cache, progress);
		
		cache.replace(comp.getUrl(), res);
		return res;
	}
	
	private ExtendedStatus getMinorStatus(Component comp, CachedStatuses cache, IProgress progress) {
		ReleaseBranchCurrent rb = reportDuration(() -> ReleaseBranchFactory.getCRB(comp), "CRB created", comp, progress);
		LinkedHashMap<Component, ExtendedStatus> subComponents = new LinkedHashMap<>();
		
		BuildStatus status;
		if (isNeedToFork(comp, rb, cache, progress, subComponents)) {
			status = BuildStatus.FORK;
		} else if (Integer.parseInt(rb.getVersion().getPatch()) > 0) {
			status = BuildStatus.DONE;
		} else if (!areMDepsLocked(rb.getMDeps())) {
			status = BuildStatus.LOCK;
		} else if (hasMDepsNotInDONEStatus(rb.getMDeps(), cache)) {
			status = BuildStatus.BUILD_MDEPS;
		} else if (!areMDepsPatchesActual(rb.getMDeps(), cache)) {
			status = BuildStatus.ACTUALIZE_PATCHES;
		} else {
			status = BuildStatus.BUILD;
		}

		Version nextVersion;
		if (status == BuildStatus.FORK) {
			nextVersion = rb.getDevVersion().toReleaseZeroPatch();
		} else {
			nextVersion = rb.getVersion();
		}
		return new ExtendedStatus(nextVersion, status, subComponents, comp);
	}

	private ExtendedStatus getPatchStatus(Component comp, CachedStatuses cache, IProgress progress) {
		ReleaseBranchPatch rb = ReleaseBranchFactory.getReleaseBranchPatch(comp);
		LinkedHashMap<Component, ExtendedStatus> subComponents = new LinkedHashMap<>();
		
		BuildStatus buildStatus;
		if (!rb.exists()) {
			throw new ENoReleaseBranchForPatch("Release Branch does not exists for the requested Component version: " + comp);
		}

		if (Integer.parseInt(rb.getVersion().getPatch()) < 1) {
			throw new ENoReleases("Release Branch version patch is " + rb.getVersion().getPatch() + ". Component release should be created before patch");
		}

		List<Component> nonlockedMDeps = new ArrayList<>();
		if (!areMDepsLocked(rb.getMDeps(), nonlockedMDeps)) {
			throw new EReleaseMDepsNotLocked(nonlockedMDeps);
		}
		
		for (Component mdep : rb.getMDeps()) {
			ExtendedStatus status = getAndCacheStatus(mdep, cache, progress, true);
			subComponents.put(mdep, status);
		}
		
		if (hasMDepsNotInDONEStatus(rb.getMDeps(), cache)) {
			buildStatus = BuildStatus.BUILD_MDEPS;
		} else if (!areMDepsPatchesActual(rb.getMDeps(), cache)) {
			buildStatus = BuildStatus.ACTUALIZE_PATCHES;
		} else if (noValueableCommitsAfterLastTag(comp, rb)) {
			buildStatus = BuildStatus.DONE;
		} else {
			buildStatus = BuildStatus.BUILD;
		}
		
		Version nextVersion = rb.getVersion();
		return new ExtendedStatus(nextVersion, buildStatus, subComponents, comp);
	}

	private boolean hasMDepsNotInDONEStatus(List<Component> mDeps, CachedStatuses cache) {
		for (Component mDep : mDeps) {
			if (cache.get(mDep.getUrl()).getStatus() != BuildStatus.DONE) {
				return true;
			}
		}
		return false;
	}

	private boolean noValueableCommitsAfterLastTag(Component comp, ReleaseBranchPatch rb) {
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
			if (!cache.get(mDep.getUrl()).getNextVersion().equals(mDep.getVersion().toNextPatch())) {
				return false;
			}
		}
		return true;
	}

	private boolean areMDepsLocked(List<Component> mDeps) {
		return areMDepsLocked(mDeps, new ArrayList<>());
	}

	private boolean areMDepsLocked(List<Component> mDeps, List<Component> nonlockedMDeps) {
		for (Component mDep : mDeps) {
			if (!mDep.getVersion().isLocked()) {
				nonlockedMDeps.add(mDep);
			}
		}
		return nonlockedMDeps.isEmpty();
	}

	private Boolean isNeedToFork(Component comp, ReleaseBranchCurrent rb, CachedStatuses cache, IProgress progress, LinkedHashMap<Component, ExtendedStatus> subComponents) {
		
		for (Component mdep : rb.getMDeps()) {
			ExtendedStatus status = getAndCacheStatus(mdep, cache, progress, false);
			subComponents.put(mdep, status);
		}
	
		if (!rb.exists()) {
			return true;
		} 

		if (rb.getVersion().getPatch().equals(Utils.ZERO_PATCH)) {
			return false;
		}
		
		// develop branch has valuable commits => YES
		if (reportDuration(() -> new DevelopBranch(comp).isModified(), "develop modified", comp, progress)) {
			return true;
		}
		
		ExtendedStatus mdepStatus;
		for (Component mdep : rb.getCRBMDeps(progress)) {
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
