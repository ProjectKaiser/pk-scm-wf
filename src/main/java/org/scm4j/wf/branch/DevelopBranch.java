package org.scm4j.wf.branch;

import org.scm4j.vcs.api.IVCS;
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
	private final IVCS vcs;
	
	public Version getVersion() {
		if (vcs.fileExists(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME)) {
			String verFileContent = vcs.getFileContent(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME);
			return new Version(verFileContent.trim());
		} 
		return null;
	}
	
	public DevelopBranchStatus getStatus() {
		List<VCSCommit> log = vcs.log(comp.getVcsRepository().getDevBranch(), 1);
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
		vcs = comp.getVcsRepository().getVcs();
	}
	
	public String getReleaseBranchName() {
		return comp.getVcsRepository().getReleaseBranchPrefix() + getVersion().toReleaseString();
	}

	public String getPreviousMinorReleaseBranchName() {
		return comp.getVcsRepository().getReleaseBranchPrefix() + getVersion().toPreviousMinorRelease();
	}
	
	public boolean hasVersionFile() {
		return vcs.fileExists(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME);
	}
	
	public String getName() {
		return comp.getVcsRepository().getDevBranch();
	}
	
	public List<Component> getMDeps() {
		if (!comp.getVcsRepository().getVcs().fileExists(getName(), SCMWorkflow.MDEPS_FILE_NAME)) {
			return new ArrayList<>();
		}
		String mDepsFileContent = vcs.getFileContent(getName(), SCMWorkflow.MDEPS_FILE_NAME);
		MDepsFile mDeps = new MDepsFile(mDepsFileContent, VCSRepositories.loadVCSRepositories());
		return mDeps.getMDeps();
	}

	@Override
	public String toString() {
		return "DevelopBranch [comp=" + comp + ", status=" + getStatus() + "]";
	}
	
	public ReleaseBranch getCurrentReleaseBranch(VCSRepositories repos) {
		DevelopBranch db = new DevelopBranch(comp);
		Version ver = db.getVersion();
		
		ReleaseBranch rb = new ReleaseBranch(comp, new Version(ver.toPreviousMinorRelease()), repos);
		ReleaseBranch oldestRB = null;
		for (int i = 0; i <= 1; i++) {
			ReleaseBranchStatus rbs = rb.getStatus();
			
			if (rbs != ReleaseBranchStatus.MISSING && rbs != ReleaseBranchStatus.BUILT && rbs != ReleaseBranchStatus.TAGGED) {
				oldestRB = rb;
			}
			rb = new ReleaseBranch(comp, new Version(ver.toPreviousMinorRelease()), repos);
		}
		return oldestRB != null ? oldestRB : new ReleaseBranch(comp, repos);
	}
}
