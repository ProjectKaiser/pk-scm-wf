package org.scm4j.releaser;

import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.DevelopBranchStatus;
import org.scm4j.releaser.conf.Component;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

public class Build {

	private final Component comp;
	private final ReleaseBranch crb;
	private final boolean isPatch;

	public Build(ReleaseBranch crb) {
		comp = crb.getComponent();
		isPatch = comp.getVersion().isExact();
		this.crb = crb;
	}
	
	public Build(Component comp) {
		this(new ReleaseBranch(comp));
	}

	public BuildStatus getStatus() {
		if (!isPatch) {
			if (isNeedToFork()) {
				DevelopBranch db = new DevelopBranch(comp);
				if (db.getStatus() == DevelopBranchStatus.IGNORED) {
					return BuildStatus.IGNORED;
				} else {
					return BuildStatus.FORK;
				}
			}
			

			Version crbVersion = crb.getHeadVersion();
			if (Integer.parseInt(crbVersion.getPatch()) > 0) {
				return BuildStatus.NONE;
			}
		} else {
			if (!crb.exists()) {
				return BuildStatus.ERROR;
			}
			
			Version headVersion = crb.getHeadVersion();
			if (Integer.parseInt(headVersion.getPatch()) < 1) {
				return BuildStatus.ERROR;
			}
		}

		List<Component> mDeps = crb.getMDeps();
		if (!areMDepsFrozen(mDeps)) {
			return BuildStatus.FREEZE;
		}

		if (!areMDepsPatchesActual(mDeps)) {
			return BuildStatus.ACTUALIZE_PATCHES;
		}
		
		if (isPatch && !hasValueableCommitsAfterLastTag()) {
			return BuildStatus.NONE;
		}

		return BuildStatus.BUILD;

	}

	private boolean hasValueableCommitsAfterLastTag() {
		IVCS vcs = comp.getVCS();
		VCSTag lastTag = vcs.getLastTag(crb.getName());
		if (lastTag == null) {
			return false;
		}
		
		List<VCSCommit> commits = vcs.getCommitsRange(crb.getName(), lastTag.getRelatedCommit().getRevision(), WalkDirection.ASC, 0);
		for (VCSCommit commit : commits) {
			if (lastTag.getRelatedCommit().equals(commit)) {
				continue;
			}
			if (!commit.getLogMessage().contains(LogTag.SCM_VER) && !commit.getLogMessage().contains(LogTag.SCM_IGNORE)) {
				return true;
			}
		}
		return false;
	}

	private boolean areMDepsPatchesActual(List<Component> mDeps) {
		for (Component mDep : mDeps) {
			ReleaseBranch crbMDep = new ReleaseBranch(mDep);
			if (!crbMDep.getHeadVersion().equals(mDep.getVersion()) && crbMDep.getHeadVersion().isGreaterThan(mDep.getVersion())) {
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
		if (!crb.exists()) {
			return true;
		}

		Version ver = crb.getHeadVersion();
		if (ver.getPatch().equals("0")) {
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

		mDeps = crb.getMDeps();
		if (mDeps.isEmpty()) {
			return false;

		}
		ReleaseBranch mDepCRB;
		for (Component mDep : mDeps) {
			mDepCRB = new ReleaseBranch(mDep);
			Version mDepCRBHeadVersion = mDepCRB.getHeadVersion();
			if (mDepCRBHeadVersion.getPatch().equals("0")) {
				if (!mDepCRBHeadVersion.equals(mDep.getVersion())) {
					return true;
				}
			} else {
				if (!mDepCRBHeadVersion.toPreviousPatch().equals(mDep.getVersion())) {
					return true;
				}
			}
		}

		return false;
	}
}
