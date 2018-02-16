package org.scm4j.releaser.exceptions;

public class ENoReleaseBranch extends EReleaserException {
	
	private static final long serialVersionUID = 1L;
	private final String releaseBranchName;

	public ENoReleaseBranch(String releaseBranchName) {
		super("branch not exists: " + releaseBranchName);
		this.releaseBranchName = releaseBranchName;
	}

	public String getReleaseBranchName() {
		return releaseBranchName;
	}
}
