package org.scm4j.releaser.branch;

import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.exceptions.EComponentConfig;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;

public class DevelopBranch {
	
	private final Component comp;
	private final IVCS vcs;
	
	public DevelopBranch(Component comp) {
		this.comp = comp;
		vcs = comp.getVCS();
	}
	
	public Version getVersion() {
		if (vcs.fileExists(comp.getVcsRepository().getDevelopBranch(), SCMReleaser.VER_FILE_NAME)) {
			String verFileContent = vcs.getFileContent(comp.getVcsRepository().getDevelopBranch(), SCMReleaser.VER_FILE_NAME, null);
			return new Version(verFileContent.trim());
		}
		throw new EComponentConfig(SCMReleaser.VER_FILE_NAME + " file is missing in develop branch of " + comp);
	}
	
	public DevelopBranchStatus getStatus() {
		List<VCSCommit> log = vcs.log(comp.getVcsRepository().getDevelopBranch(), 1);
		if (log.isEmpty()) {
			return DevelopBranchStatus.IGNORED;
		}
		VCSCommit lastCommit = log.get(0);
		if (lastCommit.getLogMessage().contains(LogTag.SCM_IGNORE)) {
			return DevelopBranchStatus.IGNORED;
		}
		if (lastCommit.getLogMessage().contains(LogTag.SCM_VER)) {
			return DevelopBranchStatus.BRANCHED;
		}
		return DevelopBranchStatus.MODIFIED;
	}
	
	public String getName() {
		return comp.getVcsRepository().getDevelopBranch();
	}

	@Override
	public String toString() {
		return "DevelopBranch [comp=" + comp + ", status=" + getStatus() + "]";
	}
}
