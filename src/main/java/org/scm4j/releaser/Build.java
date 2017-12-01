package org.scm4j.releaser;

import java.util.List;

import org.scm4j.commons.Version;
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

public class Build {

	public static final String ZERO_PATCH = "0";
	private static final int COMMITS_RANGE_LIMIT = 10;
	private final Component comp;
	private final WorkingBranch rb;
	private final DevelopBranch db;
	private final CalculatedResult calculatedResult;
	private final IProgress progress;

	public Build(WorkingBranch rb, Component comp, CalculatedResult calculatedResult) {
		this.comp = comp;
		this.rb = rb;
		db = new DevelopBranch(comp);
		this.calculatedResult = calculatedResult;
		progress = new ProgressConsole();
	}

	public Build(Component comp, WorkingBranch rb, CalculatedResult calculatedResult, IProgress progress) {
		this.comp = comp;
		this.rb = rb;
		this.db = new DevelopBranch(comp);
		this.calculatedResult = calculatedResult;
		this.progress = progress;
	}
	
//	public Build(Component comp) {
//		this(comp, new ReleaseBrnew CalculatedResult(), new ProgressConsole());
//	}

	public Build(WorkingBranch rb, Component comp) {
		this(rb, comp, new CalculatedResult());
	}

	public BuildStatus getStatus() {
		if (Options.isPatch()) {
			if (!rb.exists()) {
				throw new ENoReleaseBranchForPatch("Release Branch does not exists for the requested Component version: " + comp);
			}
			
			Version releaseVersion = rb.getNextVersion();
			if (Integer.parseInt(releaseVersion.getPatch()) < 1) {
				throw new ENoReleases("Release Branch version patch is " + releaseVersion.getPatch() + ". Component release should be created before patch");
			}
		} else {
			if (!comp.getVersion().isExact()) {
				if (isNeedToFork()) {
					return BuildStatus.FORK;
				}
			}
			
			if (Integer.parseInt(rb.getNextVersion().getPatch()) > 0) {
				return BuildStatus.DONE;
			}
		}
		
		List<Component> mDeps = calculatedResult.setMDeps(comp, rb::getMDeps, progress);
		
		if (!areMDepsFrozen(mDeps)) {
			return BuildStatus.LOCK;
		}
		
		if (hasMDepsNotInDONEStatus(mDeps)) {
			return BuildStatus.BUILD_MDEPS;
		}

		if (!areMDepsPatchesActual(mDeps)) {
			return BuildStatus.ACTUALIZE_PATCHES;
		}
		
		if (Options.isPatch() && noValueableCommitsAfterLastTag()) {
			return BuildStatus.DONE;
		}
		
		return BuildStatus.BUILD;
	}

	private boolean hasMDepsNotInDONEStatus(List<Component> mDeps) {
		for (Component mDep : mDeps) {
			
			WorkingBranch crbMDep = calculatedResult.setReleaseBranch(comp, () -> ExtendedStatusTreeBuilder.getCRB(mDep, calculatedResult, progress), progress);
			List<Component> crbMDeps = calculatedResult.getMDeps(comp);
			
			if (hasMDepsNotInDONEStatus(crbMDeps)) {
				return true;
			}
			
			BuildStatus bs = calculatedResult.setBuildStatus(mDep, () -> new Build(mDep, crbMDep, calculatedResult, progress).getStatus(),
					progress);
			if (bs != BuildStatus.DONE) {
				return true;
			}
		}
		return false;
	}

	private boolean noValueableCommitsAfterLastTag() {
		IVCS vcs = comp.getVCS();
		String startingFromRevision = null;
		
		DelayedTagsFile dtf = new DelayedTagsFile();
		String delayedTagRevision = dtf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
		List<VCSCommit> commits;
		do {
			commits = vcs.getCommitsRange(rb.getName(), startingFromRevision, WalkDirection.DESC, COMMITS_RANGE_LIMIT);
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

	private boolean areMDepsPatchesActual(List<Component> mDeps) {
		for (Component mDep : mDeps) {
			WorkingBranch rbMDep = calculatedResult.getReleaseBranch(mDep); // already created above
			// mdep 2.59.0, rb 2.59.1 - all is ok. not need to build 2.59.1 because if so BUILD_MDEPS will be result before
			if (!rbMDep.getNextVersion().equals(mDep.getVersion().toNextPatch())) { 
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

	public boolean isNeedToFork() {
		
		if (!rb.exists()) {
			return true;
		}

		Version ver = rb.getNextVersion();
		if (ver.getPatch().equals(ZERO_PATCH)) {
			return false;
		}

		if (db.getStatus() == DevelopBranchStatus.MODIFIED) {
			return true;
		}

		List<Component> mDeps = rb.getMDeps();
		WorkingBranch mDepRB;
		Version mDepRBHeadVersion;
		for (Component mDep : mDeps) {
			Boolean isNeedToForkMDep = calculatedResult.setNeedsToFork(mDep, () -> new Build(mDep, rb, calculatedResult, progress).isNeedToFork(),
					progress);
			if (isNeedToForkMDep) {
				return true;
			}
			
			mDepRB = calculatedResult.setReleaseBranch(mDep, () -> ExtendedStatusTreeBuilder.getCRB(mDep, calculatedResult, progress), progress);
			mDepRBHeadVersion = mDepRB.getNextVersion();
			// zero patch is checked above
			if (!mDepRBHeadVersion.toPreviousPatch().equals(mDep.getVersion())) {
				return true;
			}
		}

		return false;
	}
}
