package org.scm4j.releaser;

import org.scm4j.commons.Version;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.DevelopBranchStatus;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

import java.util.List;

public class Build {

	private final Component comp;
	private final ReleaseBranch rb;
	private final boolean isPatch;

	public Build(ReleaseBranch rb) {
		comp = rb.getComponent();
		isPatch = comp.getVersion().isExact();
		this.rb = rb;
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
			

			Version rbVersion = rb.getVersion();
			if (Integer.parseInt(rbVersion.getPatch()) > 0) {
				return BuildStatus.NONE;
			}
		} else {
			if (!rb.exists()) {
				return BuildStatus.ERROR;
			}
			
			Version headVersion = rb.getVersion();
			if (Integer.parseInt(headVersion.getPatch()) < 1) {
				return BuildStatus.ERROR;
			}
		}

		List<Component> mDeps = rb.getMDeps();
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
		VCSTag lastTag = vcs.getLastTag(rb.getName());
		if (lastTag == null) {
			return false;
		}
		
		List<VCSCommit> commits = vcs.getCommitsRange(rb.getName(), lastTag.getRelatedCommit().getRevision(), WalkDirection.ASC, 0);
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
			ReleaseBranch rbMDep = new ReleaseBranch(mDep);
			if (!rbMDep.getVersion().equals(mDep.getVersion()) && rbMDep.getVersion().isGreaterThan(mDep.getVersion())) {
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

		mDeps = rb.getMDeps();
		if (mDeps.isEmpty()) {
			return false;

		}
		ReleaseBranch mDeprb;
		for (Component mDep : mDeps) {
			mDeprb = new ReleaseBranch(mDep);
			Version mDeprbHeadVersion = mDeprb.getVersion();
			if (mDeprbHeadVersion.getPatch().equals("0")) {
				if (!mDeprbHeadVersion.equals(mDep.getVersion())) {
					return true;
				}
			} else {
				if (!mDeprbHeadVersion.toPreviousPatch().equals(mDep.getVersion())) {
					return true;
				}
			}
		}

		return false;
	}
}
