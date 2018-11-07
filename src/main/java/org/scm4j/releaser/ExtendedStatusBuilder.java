package org.scm4j.releaser;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranchCurrent;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.branch.ReleaseBranchPatch;
import org.scm4j.releaser.conf.*;
import org.scm4j.releaser.exceptions.EBuildStatus;
import org.scm4j.releaser.exceptions.EMinorUpgradeDowngrade;
import org.scm4j.releaser.exceptions.ENoReleaseBranchForPatch;
import org.scm4j.releaser.exceptions.ENoReleases;
import org.scm4j.releaser.exceptions.EReleaseMDepsNotLocked;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.scm4j.releaser.Utils.reportDuration;

public class ExtendedStatusBuilder {

	private static final int PARALLEL_CALCULATION_AWAIT_TIME = 500;
	private static final int COMMITS_RANGE_LIMIT = 10;
	
	private final VCSRepositoryFactory repoFactory;
	
	public ExtendedStatusBuilder(VCSRepositoryFactory repoFactory) {
		this.repoFactory = repoFactory;
	}

	public ExtendedStatus getAndCacheMinorStatus(Component comp, CachedStatuses cache) {
		return getAndCacheStatus(comp, cache, new ProgressConsole(), false);
	}

	public ExtendedStatus getAndCacheMinorStatus(String coords, CachedStatuses cache) {
		return getAndCacheMinorStatus(new Component(coords), cache);
	}
	
	public ExtendedStatus getAndCachePatchStatus(Component comp, CachedStatuses cache) {
		return getAndCacheStatus(comp, cache, new ProgressConsole(), true);
	}
	
	public ExtendedStatus getAndCachePatchStatus(String coords, CachedStatuses cache) {
		Component comp = new Component(coords);
		return getAndCachePatchStatus(comp, cache);
	}

	public ExtendedStatus getAndCacheStatus(Component comp, CachedStatuses cache, IProgress progress, boolean patch) {
		VCSRepository repo = null;
		try {
			repo = repoFactory.getVCSRepository(comp);
			ExtendedStatus existing = cache.putIfAbsent(repo.getUrl(), ExtendedStatus.DUMMY);
			
			while (ExtendedStatus.DUMMY == existing) {
				try {
					Thread.sleep(PARALLEL_CALCULATION_AWAIT_TIME);
					existing = cache.get(repo.getUrl());
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			
			if (null != existing) {
				return new ExtendedStatus(existing.getNextVersion(), existing.getStatus(), existing.getSubComponents(), comp, repo);
			}
			
			DelayedTagsFile dtf = new DelayedTagsFile();
			DelayedTag dt = dtf.getDelayedTagByUrl(repo.getUrl());
	
			ExtendedStatus res = patch ? 
				getPatchStatus(comp, cache, progress, repo, dt) :
				getMinorStatus(comp, cache, progress, repo, dt);
			
			cache.replace(repo.getUrl(), res);
			return res;
		} catch (Exception e) {
			if (repo != null) {
				cache.remove(repo.getUrl());
			}
			if (e instanceof EReleaserException) {
				throw e;
			}
			throw new EBuildStatus(e, comp);
		}
	}
	
	ExtendedStatus getMinorStatus(Component comp, CachedStatuses cache, IProgress progress, VCSRepository repo, DelayedTag dt) {
		ReleaseBranchCurrent rb = reportDuration(() -> ReleaseBranchFactory.getCRB(repo), "CRB created", comp, progress);
		Boolean hasDelayedTag = dt != null && rb.getName().equals(Utils.getReleaseBranchName(repo,  dt.getVersion()));
		LinkedHashMap<Component, ExtendedStatus> subComponents = new LinkedHashMap<>();
		
		BuildStatus status;
		if (comp.getVersion().isLocked()) {
			ConcurrentHashMap<Component, ExtendedStatus> subComponentsLocal = new ConcurrentHashMap<>();
			recursiveGetAndCacheStatusAsync(rb, cache, progress, repo, subComponentsLocal);

			for (Component mdep : rb.getMDeps()) {
				subComponents.put(mdep, subComponentsLocal.get(mdep));
			}
		}
		if (!comp.getVersion().isLocked() && isNeedToFork(comp, rb, cache, progress, subComponents, repo, hasDelayedTag)) {
			status = BuildStatus.FORK;
		} else if (Integer.parseInt(rb.getVersion().getPatch()) > 0 || hasDelayedTag) {
			status = BuildStatus.DONE;
		} else if (!areMDepsLocked(rb.getMDeps())) {
			status = BuildStatus.LOCK;
		} else if (hasMDepsNotInDONEStatus(rb.getMDeps(), cache)) {
			status = BuildStatus.BUILD_MDEPS;
		} else if (!areMDepsPatchesActualForMinor(rb.getMDeps(), cache)) {
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
		return new ExtendedStatus(nextVersion, status, subComponents, comp, repo);
	}

	void recursiveGetAndCacheStatus(CachedStatuses cache, IProgress progress,
			ConcurrentHashMap<Component, ExtendedStatus> subComponentsLocal, Component mdep, Boolean isPatch) throws RuntimeException {
		ExtendedStatus exStatus = getAndCacheStatus(mdep, cache, progress, isPatch);
		subComponentsLocal.put(mdep, exStatus);
	}

	private ExtendedStatus getPatchStatus(Component comp, CachedStatuses cache, IProgress progress, VCSRepository repo, DelayedTag dt) {
		ReleaseBranchPatch rb = reportDuration(() -> ReleaseBranchFactory.getReleaseBranchPatch(comp.getVersion(), repo),
				"RB created", comp, progress);
		Boolean hasDelayedTag = dt != null && rb.getName().equals(Utils.getReleaseBranchName(repo, dt.getVersion()));
		LinkedHashMap<Component, ExtendedStatus> subComponents = new LinkedHashMap<>();
		
		BuildStatus buildStatus;
		if (!rb.exists()) {
			throw new ENoReleaseBranchForPatch("Release Branch does not exists for the requested Component version: " + comp);
		}

		if (Integer.parseInt(rb.getVersion().getPatch()) < 1) {
			throw new ENoReleases("Release Branch version patch is " + rb.getVersion().getPatch() + ". Component release should be created before patch");
		}
		
		List<Component> nonLockedMDeps = new ArrayList<>();
		if (!areMDepsLocked(rb.getMDeps(), nonLockedMDeps)) {
			throw new EReleaseMDepsNotLocked(nonLockedMDeps);
		}
		
		ConcurrentHashMap<Component, ExtendedStatus> subComponentsLocal = new ConcurrentHashMap<>();
		Utils.async(rb.getMDeps(), (mdep) -> recursiveGetAndCacheStatus(cache, progress, subComponentsLocal, mdep, true));
		for (Component mdep : rb.getMDeps()) {
			subComponents.put(mdep, subComponentsLocal.get(mdep));
		}
		
		if (hasMDepsNotInDONEStatus(rb.getMDeps(), cache)) {
			buildStatus = BuildStatus.BUILD_MDEPS;
		} else if (!areMDepsPatchesActualForPatch(comp, repo, rb.getMDeps(), cache)) {
			buildStatus = BuildStatus.ACTUALIZE_PATCHES;
		} else if (reportDuration(() -> noValueableCommitsAfterLastTag(repo, rb), "is release branch modified check", comp, progress)) {
			buildStatus = BuildStatus.DONE;
		} else {
			buildStatus = BuildStatus.BUILD;
		}

		Version nextVersion = rb.getVersion();
		if (hasDelayedTag) {
			nextVersion = nextVersion.toNextPatch();
		}
		
		return new ExtendedStatus(nextVersion, buildStatus, subComponents, comp, repo);
	}

	private boolean hasMDepsNotInDONEStatus(List<Component> mDeps, CachedStatuses cache) {
		for (Component mDep : mDeps) {
			if (cache.get(repoFactory.getUrl(mDep)).getStatus() != BuildStatus.DONE) {
				return true;
			}
		}
		return false;
	}

	private boolean noValueableCommitsAfterLastTag(VCSRepository repo, ReleaseBranchPatch rb) {
		IVCS vcs = repo.getVCS();
		DelayedTagsFile dtf = new DelayedTagsFile();
		DelayedTag delayedTag = dtf.getDelayedTagByUrl(repo.getUrl());
		Boolean res = walkOnCommits(repo, rb, (commit) -> {
			if (delayedTag != null && commit.getRevision().equals(delayedTag.getRevision())) {
				return true;
			}
			List<VCSTag> tags = vcs.getTagsOnRevision(commit.getRevision());
			if (!commit.getLogMessage().contains(Constants.SCM_VER) && !commit.getLogMessage().contains(Constants.SCM_IGNORE)) {
				return !tags.isEmpty();
			}
			if (!tags.isEmpty()) {
				// tested by testDelayedTagOnPatch
				return true;
			}
			return null;
		});
		return res == null ? true : res;
	}
	
	private <T> T walkOnCommits(VCSRepository repo, ReleaseBranchPatch rb, Function<VCSCommit, T> func) {
		IVCS vcs = repo.getVCS();
		String startingFromRevision = null;

		List<VCSCommit> commits;
		String branchName = rb.getName();
		do {
			commits = vcs.getCommitsRange(branchName, startingFromRevision, WalkDirection.DESC, COMMITS_RANGE_LIMIT);
			for (VCSCommit commit : commits) {
				T res = func.apply(commit);
				if (res != null) {
					return res;
				}
				startingFromRevision = commit.getRevision();
			}
		} while (commits.size() >= COMMITS_RANGE_LIMIT);
		return null;
	}
	
	private boolean areMDepsPatchesActualForMinor(List<Component> mDeps, CachedStatuses cache) {
		for (Component mDep : mDeps) {
			String url = repoFactory.getUrl(mDep);
			Version nextMDepVersion = cache.get(url).getNextVersion();
			if (!nextMDepVersion.equals(mDep.getVersion().toNextPatch())) {
				DelayedTagsFile mdf = new DelayedTagsFile();
				if (!(nextMDepVersion.getPatch().equals(Constants.ZERO_PATCH) && mdf.getDelayedTagByUrl(url) != null)) {
					return false;
				}
			}
		}
		return true;
	}
	
	private boolean areMDepsPatchesActualForPatch(Component rootComp, VCSRepository repo, List<Component> mDeps, CachedStatuses cache) {
		for (Component mDep : mDeps) {
			String url = repoFactory.getUrl(mDep);
			Version nextVersion = cache.get(url).getNextVersion();
			
			// Any component `nextVersion`.truncatePatch is not equal to one mentioned in `mdeps` -> error (disallow minor upgrade/downgrade)
			if (!nextVersion.toReleaseNoPatch().equals(mDep.getVersion().toReleaseNoPatch())) {
				throw new EMinorUpgradeDowngrade(rootComp, mDep, nextVersion.toPreviousPatch());
			}
			
			DelayedTagsFile mdf = new DelayedTagsFile();
			Version verToActualizeOn ;
			if (mdf.getDelayedTagByUrl(url) != null) { // if delayed tag
				verToActualizeOn = nextVersion;
			} else {
				verToActualizeOn = nextVersion.toPreviousPatch();
			}
			
			Integer mDepPatch = Integer.parseInt(mDep.getVersion().getPatch());
			Integer nextVersionPatch = Integer.parseInt(verToActualizeOn.getPatch());
			
			// Any component `nextVersion`.patch is less than one mentioned in `mdeps` -> error (patch upgrade only is allowed)
			if (nextVersionPatch < mDepPatch) {
				throw new EMinorUpgradeDowngrade(rootComp, mDep, verToActualizeOn);
			}
			
			if (nextVersionPatch > mDepPatch) {
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

	private Boolean isNeedToFork(Component comp, ReleaseBranchCurrent rb, CachedStatuses cache, IProgress progress,
								 LinkedHashMap<Component, ExtendedStatus> subComponents, VCSRepository repo, Boolean hasDelayedTag) {
		
		ConcurrentHashMap<Component, ExtendedStatus> subComponentsLocal = new ConcurrentHashMap<>();
		recursiveGetAndCacheStatusAsync(rb, cache, progress, repo, subComponentsLocal);

		for (Component mdep : rb.getMDeps()) {
			subComponents.put(mdep, subComponentsLocal.get(mdep));
		}
	
		if (!rb.exists()) {
			return true;
		} 

		if (rb.getVersion().getPatch().equals(Constants.ZERO_PATCH)) {
			if (!hasDelayedTag) {
				return false;
			}
		}
		
		// develop branch has valuable commits => YES
		if (reportDuration(() -> new DevelopBranch(comp, repo).isModified(), "is develop modified check", comp, progress)) {
			return true;
		}
		
		for (Component mdep : rb.getCRBMDeps(progress, repo, comp)) {
			ExtendedStatus mdepStatus = cache.get(repoFactory.getUrl(mdep));
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

	private void recursiveGetAndCacheStatusAsync(ReleaseBranchCurrent rb, CachedStatuses cache, IProgress progress, VCSRepository repo, ConcurrentHashMap<Component, ExtendedStatus> subComponentsLocal) {
		Utils.async(rb.getMDeps(), (mdep) -> recursiveGetAndCacheStatus(cache, progress, subComponentsLocal, mdep, false));
	}
}
