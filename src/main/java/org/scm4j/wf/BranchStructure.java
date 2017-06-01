package org.scm4j.wf;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;

public class BranchStructure {
	private String branchName;
	private IVCS vcs;
	private Boolean hasFeatures = false;
	
	public BranchStructure(IVCS vcs, String branchName) {
		this.vcs = vcs;
		this.branchName = branchName;
		getStructure();
	}
	
	public Boolean getHasFeatures() {
		return hasFeatures;
	}
	
	private void getStructure() {
		VCSCommit headCommit = vcs.getHeadCommit(branchName);
		if (headCommit.getLogMessage().contains(SCMActionProductionRelease.VCS_TAG_SCM_IGNORE)) {
			hasFeatures = false;
		} else if(headCommit.getLogMessage().contains(SCMActionProductionRelease.VCS_TAG_SCM_VER)) {
			hasFeatures = false;
		} else {
			hasFeatures = true;
		}
	}

	@Override
	public String toString() {
		return "BranchStructure [branchName=" + branchName + ", hasFeatures=" + hasFeatures + "]";
	}
}
