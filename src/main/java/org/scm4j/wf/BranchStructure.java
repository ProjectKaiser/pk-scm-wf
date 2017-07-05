package org.scm4j.wf;


import java.util.List;

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
		if (headCommit.getLogMessage().contains(SCMActionProductionRelease.VCS_TAG_SCM_IGNORE)) {
			hasFeatures = false;
		} else if(headCommit.getLogMessage().contains(SCMActionProductionRelease.VCS_TAG_SCM_VER)) {
			hasFeatures = false;
		} else {
			hasFeatures = true;
		}
		
		List<VCSTag> tags = vcs.getTags();
		if (!tags.isEmpty() && tags.get(tags.size() - 1).getRelatedCommit().equals(vcs.getHeadCommit(branchName))) {
			releaseTag = tags.get(tags.size() - 1);
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
