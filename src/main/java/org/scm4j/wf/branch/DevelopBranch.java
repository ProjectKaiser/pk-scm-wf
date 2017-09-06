package org.scm4j.wf.branch;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.Version;

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
		if (vcs.fileExists(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME)) {
			String verFileContent = vcs.getFileContent(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME, null);
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
		return vcs.fileExists(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME);
	}
	
	public String getName() {
		return comp.getVcsRepository().getDevBranch();
	}
	
	public List<Component> getMDeps() {
		if (!comp.getVCS().fileExists(getName(), SCMWorkflow.MDEPS_FILE_NAME)) {
			return new ArrayList<>();
		}
		String mDepsFileContent = vcs.getFileContent(getName(), SCMWorkflow.MDEPS_FILE_NAME, null);
		MDepsFile mDeps = new MDepsFile(mDepsFileContent);
		return mDeps.getMDeps();
	}

	@Override
	public String toString() {
		return "DevelopBranch [comp=" + comp + ", status=" + getStatus() + "]";
	}
}
