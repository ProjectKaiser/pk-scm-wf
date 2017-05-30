package org.scm4j.wf;

import java.util.List;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;

public class BranchStructure {
	private VCSCommit lastVerCommit;
	private String branchName;
	private IVCS vcs;
	private Boolean hasFeatures = false;
	
	public BranchStructure(IVCS vcs, String branchName) {
		this.vcs = vcs;
		this.branchName = branchName;
		getStructure();
	}
	
	public VCSCommit getLastVerCommit() {
		return lastVerCommit;
	}
	
	public Boolean getHasFeatures() {
		return hasFeatures;
	}
	
	private void getStructure() {
		String headCommitId = vcs.getHeadCommit(branchName).getId();
		do {
			List<VCSCommit> commits = vcs.getCommitsRange(branchName, headCommitId, null, 10);
			for (VCSCommit commit : commits) {
				if (commit.getLogMessage().contains(SCMActionProductionRelease.VCS_TAG_SCM_VER)) {
					lastVerCommit = commit;
					break;
				} else if (!commit.getLogMessage().contains(SCMActionProductionRelease.VCS_TAG_SCM_IGNORE)) {
					hasFeatures = true;
					break;
				}	
			}
			headCommitId = commits.get(commits.size() - 1).getId();
		} while (!hasFeatures && lastVerCommit == null);  
	}

	@Override
	public String toString() {
		return "BranchStructure [branchName=" + branchName + ", lastVerCommit=" + lastVerCommit + ", hasFeatures="
				+ hasFeatures + "]";
	}
}
