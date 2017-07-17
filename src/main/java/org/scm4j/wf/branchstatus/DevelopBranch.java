package org.scm4j.wf.branchstatus;

import java.util.List;

import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.Version;

public class DevelopBranch {
	
	private final Component comp;
	private final Version version;
	
	public Version getVersion() {
		return version;
	}
	
	public DevelopBranchStatus getStatus() {
		// Latest, for develop status and what was made in release (array of Objects?)
		List<VCSCommit> log = comp.getVcsRepository().getVcs().log(comp.getVcsRepository().getDevBranch(), 1);
		if (log != null && !log.isEmpty()) {
			VCSCommit lastCommit = log.get(0);
			if (lastCommit.getLogMessage().contains(LogTag.SCM_IGNORE)) {
				return DevelopBranchStatus.IGNORED;
			} else if (lastCommit.getLogMessage().contains(LogTag.SCM_VER)) {
				return DevelopBranchStatus.BRANCHED;
			} 
		}
		return DevelopBranchStatus.MODIFIED; // status if no commits?
	}
	
	public DevelopBranch(Component comp) {
		this.comp = comp;
		String verFileContent = comp.getVcsRepository().getVcs().getFileContent(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME);
		version = new Version(verFileContent.trim());
	}

	public String getReleaseBranchName() {
		return comp.getVcsRepository().getReleaseBranchPrefix() + getVersion().toReleaseString();
	}

	public String getPreviousMinorReleaseBranchName() {
		return comp.getVcsRepository().getReleaseBranchPrefix() + getVersion().toPreviousMinorRelease();
	}

}
