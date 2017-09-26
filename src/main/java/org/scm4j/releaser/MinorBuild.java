package org.scm4j.releaser;

import org.scm4j.commons.Version;
import org.scm4j.releaser.branch.CurrentReleaseBranch;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.DevelopBranchStatus;
import org.scm4j.releaser.conf.Component;

import java.util.List;

public class MinorBuild {

	private final Component comp;
	private final CurrentReleaseBranch crb;

	public MinorBuild(CurrentReleaseBranch crb) {
		comp = crb.getComponent();
		this.crb = crb;
	}
	
	public MinorBuild(Component comp) {
		this(new CurrentReleaseBranch(comp));
	}

	public MinorBuildStatus getStatus() {
		if (isNeedToFork()) {
			return MinorBuildStatus.FORK;
		}

		Version crbVersion = crb.getVersion();
		if (Integer.parseInt(crbVersion.getPatch()) > 0) {
			return MinorBuildStatus.NONE;
		}

		List<Component> mDeps = crb.getMDeps();
		if (!areMDepsFrozen(mDeps)) {
			return MinorBuildStatus.FORK;
		}

		if (!areMDepsPatchesActual(mDeps)) {
			return MinorBuildStatus.ACTUALIZE_PATCHES;
		}

		return MinorBuildStatus.BUILD;

	}

	private boolean areMDepsPatchesActual(List<Component> mDeps) {
		for (Component mDep : mDeps) {
			CurrentReleaseBranch crbMDep = new CurrentReleaseBranch(mDep);
			if (!crbMDep.getVersion().equals(mDep.getVersion()) && crbMDep.getVersion().isGreaterThan(mDep.getVersion())) {
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
			MinorBuild mbMDep = new MinorBuild(mDep);
			if (mbMDep.isNeedToFork()) {
				return true;
			}
		}

		mDeps = crb.getMDeps();
		if (mDeps.isEmpty()) {
			return false;

		}
		CurrentReleaseBranch mDepCRB;
		for (Component mDep : mDeps) {
			mDepCRB = new CurrentReleaseBranch(mDep);
			if (!mDepCRB.getVersion().equals(mDep.getVersion())) {
				return true;
			}
		}

		return false;
	}
}
