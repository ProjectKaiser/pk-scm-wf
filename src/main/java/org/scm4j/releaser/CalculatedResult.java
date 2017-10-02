package org.scm4j.releaser;

import java.util.List;

import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;

public class CalculatedResult {

	private final ReleaseBranch releaseBranch;
	private final BuildStatus buildStatus;
	private final List<Component> mDeps;

	public CalculatedResult(ReleaseBranch releaseBranch, BuildStatus buildStatus, List<Component> mDeps) {
		this.releaseBranch = releaseBranch;
		this.buildStatus = buildStatus;
		this.mDeps = mDeps;
	}

	public ReleaseBranch getReleaseBranch() {
		return releaseBranch;
	}

	public BuildStatus getBuildStatus() {
		return buildStatus;
	}

	public List<Component> getMDeps() {
		return mDeps;
	}

}
