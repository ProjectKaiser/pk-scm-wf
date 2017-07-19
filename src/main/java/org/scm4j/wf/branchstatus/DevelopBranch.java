package org.scm4j.wf.branchstatus;

import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.conf.Version;

import java.util.ArrayList;
import java.util.List;

public class DevelopBranch {
	
	private final Component comp;
	private final Version version;
	
	public Version getVersion() {
		return version;
	}
	
	public DevelopBranchStatus getStatus() {
		List<VCSCommit> log = comp.getVcsRepository().getVcs().log(comp.getVcsRepository().getDevBranch(), 1);
		if (log == null || log.isEmpty()) {
			return DevelopBranchStatus.IGNORED; // status if no commits?
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
	
	public DevelopBranch(Component comp) {
		this.comp = comp;
		if (comp.getVcsRepository().getVcs().fileExists(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME)) {
			String verFileContent = comp.getVcsRepository().getVcs().getFileContent(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME);
			version = new Version(verFileContent.trim());
		} else {
			version = null;
		}
	}
	
	public String getReleaseBranchName() {
		return comp.getVcsRepository().getReleaseBranchPrefix() + getVersion().toReleaseString();
	}

	public String getPreviousMinorReleaseBranchName() {
		return comp.getVcsRepository().getReleaseBranchPrefix() + getVersion().toPreviousMinorRelease();
	}
	
	public boolean hasVersionFile() {
		return comp.getVcsRepository().getVcs().fileExists(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME);
	}
	
	public String getName() {
		return comp.getVcsRepository().getDevBranch();
	}
	
	public List<Component> getMDeps() {
		if (!comp.getVcsRepository().getVcs().fileExists(getName(), SCMWorkflow.MDEPS_FILE_NAME)) {
			return new ArrayList<>();
		}
		String mDepsFileContent = comp.getVcsRepository().getVcs().getFileContent(getName(), SCMWorkflow.MDEPS_FILE_NAME);
		MDepsFile mDeps = new MDepsFile(mDepsFileContent, VCSRepositories.loadVCSRepositories());
		return mDeps.getMDeps();
	}

	@Override
	public String toString() {
		return "DevelopBranch [comp=" + comp + "]";
	}
}
