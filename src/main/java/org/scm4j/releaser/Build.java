package org.scm4j.releaser;

import org.scm4j.commons.Version;
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

import java.util.List;

public class Build {

	public static final String ZERO_PATCH = "0";
	private static final int COMMITS_RANGE_LIMIT = 10;
	private final Component comp;
	private final ReleaseBranch rb;

	public Build(ReleaseBranch rb) {
		comp = rb.getComponent();
		this.rb = rb;
	}

	public Build(Component comp) {
		this.comp = comp;
		this.rb = new ReleaseBranch(comp);
	}

	public BuildStatus getStatus() {
		if (Options.isPatch()) {
			if (!rb.exists()) {
				throw new ENoReleaseBranchForPatch("Release Branch does not exists for the requested Component version: " + comp);
			}
			
			Version releaseVersion = rb.getVersion();
			if (Integer.parseInt(releaseVersion.getPatch()) < 1) {
				throw new ENoReleases("Release Branch version patch is " + releaseVersion.getPatch() + ". Component release should be created before patch");
			}
		} else {
			if (!comp.getVersion().isExact()) {
				if (isNeedToFork()) {
					return BuildStatus.FORK;
				}
			}
			
			if (Integer.parseInt(rb.getVersion().getPatch()) > 0) {
				return BuildStatus.DONE;
			}
		}
		
		List<Component> mDeps = rb.getMDeps();
		if (!areMDepsFrozen(mDeps)) {
			return BuildStatus.FREEZE;
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
			ReleaseBranch rbMDep = new ReleaseBranch(mDep);
			if (hasMDepsNotInDONEStatus(rbMDep.getMDeps())) {
				return true;
			}
			Build bMDep = new Build(mDep);
			if (bMDep.getStatus() != BuildStatus.DONE) {
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
			ReleaseBranch rbMDep = new ReleaseBranch(mDep);
			// mdep 2.59.0, rb 2.59.1 - all is ok. not need to build 2.59.1 because if so BUILD_MDEPS will be result before
			if (!rbMDep.getVersion().equals(mDep.getVersion().toNextPatch())) { 
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

		Version ver = rb.getVersion();
		if (ver.getPatch().equals(ZERO_PATCH)) {
			return false;
		}

		DevelopBranch db = new DevelopBranch(comp);
		if (db.getStatus() == DevelopBranchStatus.MODIFIED) {
			return true;
		}

		List<Component> mDeps = db.getMDeps();
		for (Component mDep : mDeps) {
			Build mbMDep = new Build(mDep);
			if (mbMDep.isNeedToFork()) {
				return true;
			}
		}

		mDeps = rb.getMDeps();
		if (mDeps.isEmpty()) {
			return false;

		}
		ReleaseBranch mDepRB;
		for (Component mDep : mDeps) {
			mDepRB = new ReleaseBranch(mDep);
			Version mDepRBHeadVersion = mDepRB.getVersion();
			// zero patch is checked above
			if (!mDepRBHeadVersion.toPreviousPatch().equals(mDep.getVersion())) {
				return true;
			}
		}

		return false;
	}
}
