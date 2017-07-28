package org.scm4j.wf.actions.results;

public class ActionResultReleaseBranchFork {

	private final String branchName;

	public String getBranchName() {
		return branchName;
	}

	public ActionResultReleaseBranchFork(String branchName) {
		super();
		this.branchName = branchName;
	}

	@Override
	public String toString() {
		return "ActionResultReleaseBranchFork [branchName=" + branchName + "]";
	}
}
