package org.scm4j.wf.branchstatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.conf.Dep;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.VCSRepositories;

public class BranchStatus {

	public BranchStatuses getCurrentStatuses (Dep dep, VCSRepositories repos) {
		// Latest, for develop status and what was made in release (array of Objects?)
		IVCS vcs = dep.getVcsRepository().getVcs();
		BranchStatuses res = new BranchStatuses();
		List<VCSCommit> log = vcs.log(dep.getVcsRepository().getDevBranch(), 1);

		res.setDevelopStatus(DevelopBranchStatus.MODIFIED); // status if no commits?
		if (log != null && !log.isEmpty()) {
			VCSCommit lastCommit = log.get(0);
			if (lastCommit.getLogMessage().contains(LogTag.SCM_IGNORE)) {
				res.setDevelopStatus(DevelopBranchStatus.IGNORED);
			} else if (lastCommit.getLogMessage().contains(LogTag.SCM_VER)) {
				res.setDevelopStatus(DevelopBranchStatus.BRANCHED);
			} 
		}
		
		res.setReleaseStatus(getReleaseStatus(dep, repos));
		return res;
	}
	
	public ReleaseBranchStatus getReleaseStatus (Dep dep, VCSRepositories repos) {
		//  status of current or latest release
		// MDEPS_TAGGED, MDEPS_FROZEN, BRANCHED
		IVCS vcs = dep.getVcsRepository().getVcs();
		String releaseBranchName = dep.getReleaseBranchName();
		Set<String> branches = vcs.getBranches();
		if (!branches.contains(releaseBranchName)) {
			return ReleaseBranchStatus.MISSING;
		}
		
		List<VCSCommit> log = vcs.log(releaseBranchName, 1);
		if (log != null && !log.isEmpty()) {
			VCSCommit lastCommit = log.get(0);
			if (lastCommit.getLogMessage().contains(LogTag.SCM_BUILT)) {
				return ReleaseBranchStatus.BUILT;
			} else {
				List<VCSTag> tags = vcs.getTags();
				for (VCSTag tag : tags) {
					if (tag.getRelatedCommit().equals(lastCommit) && tag.getTagName().equals(dep.getActualVersion().toPreviousMinorRelease())) {
						return ReleaseBranchStatus.TAGGED;
					}
				}
			}
		} else {
			return ReleaseBranchStatus.BRANCHED;
		}
		
		String devBranchName = dep.getVcsRepository().getDevBranch();
		vcs = dep.getVcsRepository().getVcs(); //VCSFactory.getIVCS(dep.getVcsRepository(), ws);
		List<Dep> mDeps = new ArrayList<>();
		if (vcs.fileExists(devBranchName, SCMWorkflow.MDEPS_FILE_NAME)) {
			String mDepsContent = vcs.getFileContent(devBranchName, SCMWorkflow.MDEPS_FILE_NAME);
			mDeps = new MDepsFile(mDepsContent, repos).getMDeps();
		}
		
		Boolean allTagged = null;
		Boolean allBranched = null;
		for (Dep mDep : mDeps) {
			ReleaseBranchStatus status = getReleaseStatus(mDep, repos);
			if (status == ReleaseBranchStatus.TAGGED) {
				if (allBranched == null) {
					allTagged = true;
				} else {
					return ReleaseBranchStatus.BRANCHED;
				}
			} else if (status == ReleaseBranchStatus.BRANCHED) {
				if (allTagged == null) {
					allBranched = true;
				} else {
					return ReleaseBranchStatus.BRANCHED;
				}
			} else {
				return ReleaseBranchStatus.BRANCHED;
			}
		}
		if (allTagged) {
			return ReleaseBranchStatus.MDEPS_TAGGED;
		} else if (allBranched) {
			return ReleaseBranchStatus.MDEPS_FROZEN;
		}
		return ReleaseBranchStatus.BRANCHED;
	}
}
