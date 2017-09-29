package org.scm4j.releaser;

import org.scm4j.commons.Version;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.DevelopBranchStatus;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.Options;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

import java.util.List;

public class Build {

	public static final String ZERO_PATCH = "0";
	private final Component comp;
	private final ReleaseBranch rb;

	public Build(ReleaseBranch rb) {
		comp = rb.getComponent();
		this.rb = rb;
	}
	
	public Build(Component comp) {
		this(new ReleaseBranch(comp));
	}

	public BuildStatus getStatus() {
		if (Options.isPatch()) {
			if (!rb.exists()) {
				return BuildStatus.ERROR;
			}
			
			Version headVersion = rb.getVersion();
			if (/*comp.isProduct() && */Integer.parseInt(headVersion.getPatch()) < 1) {
				return BuildStatus.ERROR;
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
			if (!rbMDep.exists()) {
				return false;
			}
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
		List<VCSCommit> commits = vcs.getCommitsRange(rb.getName(), null, WalkDirection.DESC, 0);
		DelayedTagsFile dtf = new DelayedTagsFile();
		String delayedTagRevision = dtf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
		for (VCSCommit commit : commits) {
			if (commit.getRevision().equals(delayedTagRevision)) {
				return true;
			}
			if (!commit.getLogMessage().contains(LogTag.SCM_VER) && !commit.getLogMessage().contains(LogTag.SCM_IGNORE)) {
				List<VCSTag> tags = vcs.getTagsOnRevision(commit.getRevision());
				return !tags.isEmpty();
			}
		}
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
		ReleaseBranch mDeprb;
		for (Component mDep : mDeps) {
			mDeprb = new ReleaseBranch(mDep);
			Version mDeprbHeadVersion = mDeprb.getVersion();
			if (mDeprbHeadVersion.getPatch().equals(ZERO_PATCH)) {
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
