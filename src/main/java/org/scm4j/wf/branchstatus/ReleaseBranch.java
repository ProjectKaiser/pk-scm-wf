package org.scm4j.wf.branchstatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.conf.Version;

public class ReleaseBranch {
	
	private final Component comp;
	private final Version version;
	private final IVCS vcs;
	private final VCSRepositories repos;
	
	public ReleaseBranch(Component comp, VCSRepositories repos) {
		this(comp, new Version(comp.getVcsRepository().getVcs().getFileContent(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME).trim()), repos);
	}
	
	public ReleaseBranch(Component comp, Version version, VCSRepositories repos) {
		this.version = version;
		this.comp = comp;
		this.repos = repos;
		vcs = comp.getVcsRepository().getVcs();
	}
	
	protected ReleaseBranchStatus getStatus () {
		if (isMissing()) {
			return ReleaseBranchStatus.MISSING;
		}
		
		if (isTagged()) {
			return ReleaseBranchStatus.TAGGED;
		}
		
		if (isComponentBuilt(comp)) {
			return ReleaseBranchStatus.BUILT;
		}
		
		if (mDepsTagged()) {
			return ReleaseBranchStatus.MDEPS_TAGGED;
		}
		
		if (mDepsFrozen()) {
			return ReleaseBranchStatus.MDEPS_FROZEN;
		}
		
		return ReleaseBranchStatus.BRANCHED;
	}

	private boolean mDepsTagged() {
		String devBranchName = comp.getVcsRepository().getDevBranch();
		List<Component> mDeps = new ArrayList<>();
		if (vcs.fileExists(devBranchName, SCMWorkflow.MDEPS_FILE_NAME)) {
			String mDepsContent = vcs.getFileContent(devBranchName, SCMWorkflow.MDEPS_FILE_NAME);
			mDeps = new MDepsFile(mDepsContent, repos).getMDeps();
		}
		if (mDeps.size() == 0) {
			return false;
		}
		
		ReleaseBranch rb;
		ReleaseBranchStatus status;
		for (Component mDep : mDeps) {
			rb = new ReleaseBranch(mDep, repos);
			status = rb.getStatus();
			if (status != ReleaseBranchStatus.TAGGED || !isComponentBuilt(mDep)) {
				return false;
			}
		}
		return true;
	}

	private boolean isTagged() {
		List<VCSCommit> log = vcs.log(getReleaseBranchName(), 1);
		if (log != null && !log.isEmpty()) { // what to return if no commits at all?
			VCSCommit lastCommit = log.get(0);
			List<VCSTag> tags = vcs.getTags();
			DevelopBranch db;
			for (VCSTag tag : tags) {
				db = new DevelopBranch(comp);
				if (tag.getRelatedCommit().equals(lastCommit) && tag.getTagName().equals(db.getVersion().toPreviousMinorRelease())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isMissing() {
		String releaseBranchName = getReleaseBranchName();
		Set<String> branches = vcs.getBranches();
		return !branches.contains(releaseBranchName);
	}

	private boolean isComponentBuilt(Component comp) {
		IVCS vcs = comp.getVcsRepository().getVcs();
		ReleaseBranch rb = new ReleaseBranch(comp, repos);
		List<VCSCommit> log = vcs.log(rb.getReleaseBranchName(), 1);
		if (log != null && !log.isEmpty()) {
			VCSCommit lastCommit = log.get(0);
			return lastCommit.getLogMessage().contains(LogTag.SCM_BUILT);
		}
		return false;
	}

	private Boolean mDepsFrozen() {
		IVCS vcs = comp.getVcsRepository().getVcs();
		try {
			String mDepsContent = vcs.getFileContent(getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME);
			List<Component> mDeps = new MDepsFile(mDepsContent, repos).getMDeps();
			if (mDeps.isEmpty()) {
				return false;
			}
			for (Component mDep : mDeps) {
				if (mDep.getVersion().getMinor().isEmpty()) {
					return false;
				}
			}
			return true;
		} catch (EVCSFileNotFound e) {
			return false;
		}
	}
	
	public String getReleaseBranchName() {
		return comp.getVcsRepository().getReleaseBranchPrefix() + version.toReleaseString();
	}

	@Override
	public String toString() {
		return "ReleaseBranch [comp=" + comp + "]";
	}
}
