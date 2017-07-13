package org.scm4j.wf;


import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;

public class BranchStructure {
	private final String branchName;
	private final Boolean hasFeatures;
	private final VCSTag releaseTag;
	
	public BranchStructure(IVCS vcs, String branchName) {
		this.branchName = branchName;
		VCSCommit headCommit = vcs.getHeadCommit(branchName);
		if (headCommit.getLogMessage().contains(LogTag.SCM_IGNORE)) {
			hasFeatures = false;
		} else if(headCommit.getLogMessage().contains(LogTag.SCM_VER)) {
			hasFeatures = false;
		} else {
			hasFeatures = true;
		}
		
		VCSTag lastTag = vcs.getLastTag();
		if (lastTag != null && lastTag.getRelatedCommit().equals(vcs.getHeadCommit(branchName))) {
			releaseTag = lastTag;
		} else {
			releaseTag = null;
		}
	}
	
	public VCSTag getReleaseTag() {
		return releaseTag;
	}
	
	public Boolean getHasFeatures() {
		return hasFeatures;
	}
	
	@Override
	public String toString() {
		return "BranchStructure [branchName=" + branchName + ", hasFeatures=" + hasFeatures + ", releaseTag="
				+ releaseTag + "]";
	}
	
}
