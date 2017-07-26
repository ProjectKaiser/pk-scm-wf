package org.scm4j.wf.branchstatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
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
		this(comp, null, repos);
	}
	
	public ReleaseBranch(Component comp, Version version, VCSRepositories repos) {
		this.version = version;
		this.comp = comp;
		this.repos = repos;
		vcs = comp.getVcsRepository().getVcs();
	}
	
	public ReleaseBranchStatus getStatus () {
		if (!exists()) {
			return ReleaseBranchStatus.MISSING;
		}
		
		if (isLastCommitTagged()) {
			return ReleaseBranchStatus.TAGGED;
		}
		
		if (isLastCommitHasSCM_BUILTLogTag(comp)) {
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
			if (status != ReleaseBranchStatus.TAGGED || !isLastCommitHasSCM_BUILTLogTag(mDep)) {
				return false;
			}
		}
		return true;
	}

	private boolean isLastCommitTagged() {
		List<VCSCommit> log = vcs.log(getReleaseBranchName(), 1);
		if (log != null && !log.isEmpty()) { // what to return if no commits at all?
			VCSCommit lastCommit = log.get(0);
			List<VCSTag> tags = vcs.getTags();
			DevelopBranch db = new DevelopBranch(comp);
			for (VCSTag tag : tags) {
				if (tag.getRelatedCommit().equals(lastCommit) && tag.getTagName().equals(db.getVersion().toPreviousMinorRelease())) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean exists() {
		String releaseBranchName = getReleaseBranchName();
		Set<String> branches = vcs.getBranches();
		return branches.contains(releaseBranchName);
	}

	private boolean isLastCommitHasSCM_BUILTLogTag(Component comp) {
		IVCS vcs = comp.getVcsRepository().getVcs();
		List<VCSCommit> log = vcs.log(getReleaseBranchName(), 1);
		if (log != null && !log.isEmpty()) {
			VCSCommit lastCommit = log.get(0);
			return lastCommit.getLogMessage().contains(LogTag.SCM_BUILT);
		}
		return false;
	}

	private Boolean mDepsFrozen() {
		IVCS vcs = comp.getVcsRepository().getVcs();
		List<Component> mDeps = new ArrayList<>();
		if (vcs.fileExists(getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME)) {
			String mDepsContent = vcs.getFileContent(getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME);
			mDeps = new MDepsFile(mDepsContent, repos).getMDeps();
		}
		if (mDeps.isEmpty()) {
			return false;
		}
		for (Component mDep : mDeps) {
			if (mDep.getVersion().getMinor().isEmpty()) {
				return false;
			}
		}
		return true;
	}
	
	public String getReleaseBranchName() {
		return comp.getVcsRepository().getReleaseBranchPrefix() + getVersion().toReleaseString();
	}

	@Override
	public String toString() {
		return "ReleaseBranch [comp=" + comp + ", targetVersion=" + getVersion() + ", status=" + getStatus() + "]";
	}

	public VCSTag getReleaseTag() {
		if (exists() || !isLastCommitTagged()) {
			return null;
		}
		
		VCSCommit releaseHeadCommit = vcs.getHeadCommit(getReleaseBranchName());
		List<VCSTag> tags = vcs.getTags();
		DevelopBranch db = new DevelopBranch(comp);
		for (VCSTag tag : tags) {
			if (tag.getRelatedCommit().equals(releaseHeadCommit) && tag.getTagName().equals(db.getVersion().toPreviousMinorRelease())) {
				return tag;
			}
		}
		throw new IllegalStateException("No tag on release branch '" + getReleaseBranchName() + "' head commit for component:" + comp);
	}
	
	public List<Component> getMDeps() {
		if (!vcs.getBranches().contains(getReleaseBranchName()) || !vcs.fileExists(getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME)) {
			return new ArrayList<>();
		}
		
		String mDepsFileContent = comp.getVcsRepository().getVcs().getFileContent(getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME);
		MDepsFile mDeps = new MDepsFile(mDepsFileContent, repos);
		return mDeps.getMDeps();
	}
	
	public Version getVersion() {
		if (version == null) {
			return new Version(vcs.getFileContent(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME).trim());
		} else {
			return version;
		}
	}
}
