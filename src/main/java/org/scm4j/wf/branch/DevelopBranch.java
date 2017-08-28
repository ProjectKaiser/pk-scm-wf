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
	
	public DevelopBranch(Component comp) {
		this.comp = comp;
		vcs = comp.getVCS();
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
		String mDepsFileContent = vcs.getFileContent(getName(), SCMWorkflow.MDEPS_FILE_NAME);
		MDepsFile mDeps = new MDepsFile(mDepsFileContent, VCSRepositories.loadVCSRepositories());
		return mDeps.getMDeps();
	}

	@Override
	public String toString() {
		return "DevelopBranch [comp=" + comp + ", status=" + getStatus() + "]";
	}
	
	public ReleaseBranch getCurrentReleaseBranch(VCSRepositories repos) {
		Version ver = getVersion();
		
		ReleaseBranch rb = new ReleaseBranch(comp, ver, repos);
		/**
		 * just built the compoennt. db is BRANCHED, and we want to see the just built release.
		 */
		
		if (getStatus() == DevelopBranchStatus.BRANCHED) {
			ReleaseBranch justBuiltRB = new ReleaseBranch(comp, ver.toPreviousMinor(), repos);
			if (justBuiltRB.getStatus() != ReleaseBranchStatus.MISSING) {
				return justBuiltRB;
			}
		}

		ReleaseBranchStatus rbs;
		ReleaseBranchStatus prevRBS = null;
		ReleaseBranch prevRB = rb;
		while (true) {
			rbs = rb.getStatus();
			if (rbs == ReleaseBranchStatus.MISSING && prevRBS != null && prevRBS == ReleaseBranchStatus.MISSING) {
				return new ReleaseBranch(comp, getVersion(), repos);
			}
			if (rbs == ReleaseBranchStatus.ACTUAL) {
				return prevRB;
			}
			if (rbs != ReleaseBranchStatus.MISSING) {
				return rb;
			}
			ver = ver.toPreviousMinor();
			if (ver.getMinor().equals("0")) {
				return rb;
			}
			prevRB = rb;
			prevRBS = rbs;
			rb = new ReleaseBranch(comp,  ver, repos);
		}
	}
}
