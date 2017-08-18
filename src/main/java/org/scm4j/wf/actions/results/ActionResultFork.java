package org.scm4j.wf.actions.results;

public class ActionResultFork {

	private final String branchName;

	public String getBranchName() {
		return branchName;
	}

	public ActionResultFork(String branchName) {
		this.branchName = branchName;
	}

	@Override
	public String toString() {
		return "ActionResultFork [branchName=" + branchName + "]";
	}
}
