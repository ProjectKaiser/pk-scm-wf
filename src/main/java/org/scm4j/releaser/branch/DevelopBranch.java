package org.scm4j.releaser.branch;

import org.scm4j.commons.Version;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;

import java.util.ArrayList;
import java.util.List;

public class DevelopBranch {
	
	private final Component comp;
	private final IVCS vcs;
	
	public DevelopBranch(Component comp) {
		this.comp = comp;
		vcs = comp.getVCS();
	}
	
	public Version getVersion() {
		if (vcs.fileExists(comp.getVcsRepository().getDevBranch(), SCMReleaser.VER_FILE_NAME)) {
			String verFileContent = vcs.getFileContent(comp.getVcsRepository().getDevBranch(), SCMReleaser.VER_FILE_NAME, null);
			return new Version(verFileContent.trim());
		}
		return null;
	}
	
	public DevelopBranchStatus getStatus() {
		List<VCSCommit> log = vcs.log(comp.getVcsRepository().getDevBranch(), 1);
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
	
	public boolean hasVersionFile() {
		return vcs.fileExists(comp.getVcsRepository().getDevBranch(), SCMReleaser.VER_FILE_NAME);
	}
	
	public String getName() {
		return comp.getVcsRepository().getDevBranch();
	}
	
	public List<Component> getMDeps() {
		if (!comp.getVCS().fileExists(getName(), SCMReleaser.MDEPS_FILE_NAME)) {
			return new ArrayList<>();
		}
		String mDepsFileContent = vcs.getFileContent(getName(), SCMReleaser.MDEPS_FILE_NAME, null);
		MDepsFile mDeps = new MDepsFile(mDepsFileContent);
		return mDeps.getMDeps();
	}

	@Override
	public String toString() {
		return "DevelopBranch [comp=" + comp + ", status=" + getStatus() + "]";
	}
}
