package org.scm4j.wf;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;

public class BranchStructure {
	private final String branchName;
	private final Boolean hasFeatures;
	
	public BranchStructure(IVCS vcs, String branchName) {
		this.branchName = branchName;
		VCSCommit headCommit = vcs.getHeadCommit(branchName);
		if (headCommit.getLogMessage().contains(SCMActionProductionRelease.VCS_TAG_SCM_IGNORE)) {
			hasFeatures = false;
		} else if(headCommit.getLogMessage().contains(SCMActionProductionRelease.VCS_TAG_SCM_VER)) {
			hasFeatures = false;
		} else {
			hasFeatures = true;
		}
	}
	
	public Boolean getHasFeatures() {
		return hasFeatures;
	}
	
	@Override
	public String toString() {
		return "BranchStructure [branchName=" + branchName + ", hasFeatures=" + hasFeatures + "]";
	}
	
}
