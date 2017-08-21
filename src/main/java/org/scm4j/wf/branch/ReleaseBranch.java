package org.scm4j.wf.branch;

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
		this(comp, new Version(comp.getVcsRepository().getVcs().getFileContent(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME).trim()),  repos);
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
		
		if (hasLastCommitSCM_BUILTLogTag(comp)) {
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
		List<Component> mDeps = getMDeps();
		if (mDeps.size() == 0) {
			return false;
		}
		
		ReleaseBranch rb;
		ReleaseBranchStatus status;
		for (Component mDep : mDeps) {
			rb = new ReleaseBranch(mDep, repos);
			status = rb.getStatus();
			if (status != ReleaseBranchStatus.TAGGED && !hasLastCommitSCM_BUILTLogTag(mDep)) {
				return false;
			}
		}
		return true;
	}

	private boolean isLastCommitTagged() {
		List<VCSCommit> log = vcs.log(getReleaseBranchName(), 1);
		if (log != null && !log.isEmpty()) {
			VCSCommit lastCommit = log.get(0);
			List<VCSTag> tags = vcs.getTags();
			DevelopBranch db = new DevelopBranch(comp);
			for (VCSTag tag : tags) {
				if (tag.getRelatedCommit().equals(lastCommit) && tag.getTagName().equals(db.getVersion().toPreviousMinor().toReleaseString())) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean exists() {
		String releaseBranchName = getReleaseBranchName();
		Set<String> branches = vcs.getBranches(comp.getVcsRepository().getReleaseBranchPrefix());
		return branches.contains(releaseBranchName);
	}

	private boolean hasLastCommitSCM_BUILTLogTag(Component comp) {
		List<VCSCommit> log = vcs.log(getReleaseBranchName(), 1);
		if (log != null && !log.isEmpty()) {
			VCSCommit lastCommit = log.get(0);
			return lastCommit.getLogMessage().contains(LogTag.SCM_BUILT);
		}
		return false;
	}

	private Boolean mDepsFrozen() {
		List<Component> mDeps = getMDeps();
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
	
	

	@Override
	public String toString() {
		return "ReleaseBranch [comp=" + comp + ", targetVersion=" + getTargetVersion().toReleaseString() + ", status=" + getStatus() + "]";
	}


	public VCSTag getReleaseTag() {
		if (exists() || !isLastCommitTagged()) {
			return null;
		}
		
		VCSCommit releaseHeadCommit = vcs.getHeadCommit(getReleaseBranchName());
		List<VCSTag> tags = vcs.getTags();
		DevelopBranch db = new DevelopBranch(comp);
		for (VCSTag tag : tags) {
			if (tag.getRelatedCommit().equals(releaseHeadCommit) && tag.getTagName().equals(db.getVersion().toPreviousMinor().toReleaseString())) {
				return tag;
			}
		}
		throw new IllegalStateException("No tag on release branch '" + getReleaseBranchName() + "' head commit for component:" + comp);
	}
	
	public List<Component> getMDeps() {
		if (!vcs.getBranches(comp.getVcsRepository().getReleaseBranchPrefix()).contains(getReleaseBranchName()) || !vcs.fileExists(getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME)) {
			return new ArrayList<>();
		}
		
		String mDepsFileContent = comp.getVcsRepository().getVcs().getFileContent(getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME);
		MDepsFile mDeps = new MDepsFile(mDepsFileContent, repos);
		return mDeps.getMDeps();
	}
	
	public String getReleaseBranchName() {
		return comp.getVcsRepository().getReleaseBranchPrefix() + version.usePatch(false).toReleaseString();
	}
	
	public Version getCurrentVersion() {
		return new Version(comp.getVcsRepository().getVcs().getFileContent(getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME).trim()).useSnapshot(false);
	}
	
	public Version getVersion() {
		return version.useSnapshot(false);
	}
	
	public Version getTargetVersion() {
		return getVersion().toNextPatch();
	}

//	private boolean isModified() {
//		if (!exists()) {
//			return false;
//		}
//		List<VCSCommit> log = vcs.log(getReleaseBranchName(), 1);
//		if (log == null || log.isEmpty()) {
//			return false;
//		}
//		VCSCommit lastCommit = log.get(0);
//		if (lastCommit.getLogMessage().contains(LogTag.SCM_IGNORE)) {
//			return false;
//		}
//		if (lastCommit.getLogMessage().contains(LogTag.SCM_VER)) {
//			return false;
//		}
//		return true;
//	}
}
