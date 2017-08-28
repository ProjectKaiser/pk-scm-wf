package org.scm4j.wf.branch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;
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
		this(comp, new Version(comp.getVCS().getFileContent(comp.getVcsRepository().getDevBranch(), SCMWorkflow.VER_FILE_NAME).trim()),  repos);
	}
	
	public ReleaseBranch(Component comp, Version version, VCSRepositories repos) {
		this.version = version.toRelease();
		this.comp = comp;
		this.repos = repos;
		vcs = comp.getVCS();
	}
	
	public ReleaseBranchStatus getStatus () {
		if (!exists()) {
			return ReleaseBranchStatus.MISSING;
		}
		
		if (mDepsFrozen()) {
			if (mDepsActual()) {
				if (isPreHeadCommitTaggedWithVersion()) {
					return ReleaseBranchStatus.ACTUAL;
				}
				return ReleaseBranchStatus.MDEPS_ACTUAL;
			}
			return ReleaseBranchStatus.MDEPS_FROZEN;
		}
		
		return ReleaseBranchStatus.BRANCHED;
	}
	
	private boolean mDepsActual() {
		List<Component> mDeps = getMDeps();
		if (mDeps.isEmpty()) {
			return true;
		}
		ReleaseBranch mDepRB;
		for (Component mDep : mDeps) {
			if (!mDep.getVersion().isExactVersion()) {
				return false;
			}
			mDepRB = new ReleaseBranch(mDep, mDep.getVersion(), repos);
			if (!mDepRB.isPreHeadCommitTaggedWithVersion()) {
				return false;
			}
		}
		return true;
	}
	
	private VCSTag getVersionTag() {
		return vcs.getTagByName(version.toReleaseString());
	}

	public boolean isPreHeadCommitTaggedWithVersion() {
		VCSTag tag = getVersionTag();
		if (tag == null) {
			return false;
		}
		// check is tagged commit is head-1
		List<VCSCommit> commits = vcs.getCommitsRange(getReleaseBranchName(), null, WalkDirection.DESC, 2);
		if (commits.size() < 2) {
			return false;
		}
		return commits.get(1).equals(tag.getRelatedCommit());
	}

	public boolean exists() {
		String releaseBranchName = getReleaseBranchName();
		Set<String> branches = vcs.getBranches(comp.getVcsRepository().getReleaseBranchPrefix());
		return branches.contains(releaseBranchName);
	}

	private Boolean mDepsFrozen() {
		List<Component> mDeps = getMDeps();
		if (mDeps.isEmpty()) {
			return true;
		}
		for (Component mDep : mDeps) {
			if (!mDep.getVersion().isExactVersion()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return "ReleaseBranch [comp=" + comp + ", version=" + version.toReleaseString() + ", status=" + getStatus() + ", name=" + getReleaseBranchName() + "]";
	}

	public VCSTag getReleaseTag() {
		if (!exists() || getVersionTag() == null) {
			return null;
		}
		
		VCSCommit releaseHeadCommit = vcs.getHeadCommit(getReleaseBranchName());
		List<VCSTag> tags = vcs.getTags();
		DevelopBranch db = new DevelopBranch(comp);
		// need last tag on release branch 
		for (VCSTag tag : tags) {
			if (tag.getRelatedCommit().equals(releaseHeadCommit) && tag.getTagName().equals(db.getVersion().toPreviousMinor().toReleaseString())) {
				return tag;
			}
		}
		throw new IllegalStateException("No tag on release branch " + getReleaseBranchName() + " head commit for component:" + comp);
	}
	
	public List<Component> getMDeps() {
		if (!vcs.getBranches(comp.getVcsRepository().getReleaseBranchPrefix()).contains(getReleaseBranchName()) || !vcs.fileExists(getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME)) {
			return new ArrayList<>();
		}
		
		String mDepsFileContent = comp.getVCS().getFileContent(getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME);
		MDepsFile mDeps = new MDepsFile(mDepsFileContent, repos);
		return mDeps.getMDeps();
	}
	
	public String getReleaseBranchName() {
		return comp.getVcsRepository().getReleaseBranchPrefix() + version.getReleaseNoPatchString();
	}
	
	public Version getCurrentVersion() {
		return new Version(comp.getVCS().getFileContent(getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME).trim());
	}
	
	public Version getVersion() {
		return version;
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
