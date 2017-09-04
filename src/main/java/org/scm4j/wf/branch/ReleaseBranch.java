package org.scm4j.wf.branch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.conf.CommitsFile;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.conf.Version;

public class ReleaseBranch {

	private final Component comp;
	private final Version version;
	private final IVCS vcs;
	private final VCSRepositories repos;
	private final String name;

	public ReleaseBranch(Component comp, Version version, VCSRepositories repos) {
		this.version = version.toRelease();
		this.comp = comp;
		this.repos = repos;
		name = computeName();
		vcs = comp.getVCS();
	}

	private String computeName() {
		return comp.getVcsRepository().getReleaseBranchPrefix() + version.getReleaseNoPatchString();
	}
	
	/*
	 * Last release we need to work with.
	 * Develop branch								Release Branch          Meaning
	 * 2.52-SNAPSHOT								2.52 MISSING            We just build the 2.51 release, no new features in Develop branch. We want to see the 2.51 release to continue building or to show it is built, i.e. result is Release Branch 2.51 
	 * BRANCHED, i.e. last commit is #scm-ver       2.51 not MISSING        
	 * 
	 * 2.52-SNAPSHOT                                2.52 MISSING            We do not have releases at all. We need to work with next possible release, i.e. result is ReleaseBranch 2.52 
	 * any state                                    2.51 MISSING            
	 * 
	 * 2.52-SNAPSHOT                                2.52 ACTUAL             We just built 2.52. We need to show 2.52 is built. Result is Release Branch 2.52.
	 * MODIFIED, IGNORED                            2.51 any state
	 * 
	 * 2.52-SNAPSHOT                                2.52 uncompleted        We need to finish 2.52. Result is Release Branch 2.52.
	 * MODIFIED, IGNORED                            2.51 any state
	 * 
	 * 2.52-SNAPSHOT                                2.52 MISSING            We built 2.51 and have modifications for 2.52. Need to fork 2.52, i.e. result is Relese Branch 2.52
	 * MODIFIED                                     2.51 ACTUAL
	 * 
	 * 2.52-SNAPSHOT                                2.52 MISSING            We need to finish version X. Release Branch X is result
	 * any state                                    2.51 MISSING
	 *                                              ...
	 *                                              (X+1) MISSING
	 *                                              X uncompleted 
	 * 
	 * uncompleted means any of MDEPS_FROZEN, MDEPS_ACTUAL, BRANCHED
	 */
	public ReleaseBranch(final Component comp, VCSRepositories repos) {
//		if (comp.getVersion().isExactVersion()) {
//			this.version = comp.getVersion().toRelease();
//			this.comp = comp;
//			this.repos = repos;
//			name = computeName();
//			vcs = comp.getVCS();
//			return;
//		}
		this.comp = comp;
		this.repos = repos;
		vcs = comp.getVCS();
		DevelopBranch db = new DevelopBranch(comp);
		Version ver = db.getVersion().toRelease();

		List<String> releaseBranches = new ArrayList<>(vcs.getBranches(comp.getVcsRepository().getReleaseBranchPrefix() + (comp.getVersion().isExactVersion() ? comp.getVersion().getReleaseNoPatchString() : "")));
		if (releaseBranches.isEmpty()) {
			this.version = ver;
			name = computeName();
			return;
		}
		
		// first is last release. If not, then 2.59 will be selected because it is completed, but 2.60 exists and Develop is BRANCHED. 2.60 is expected. 
		Collections.sort(releaseBranches, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				Version ver1 = new Version(o1.replace(comp.getVcsRepository().getReleaseBranchPrefix(), ""));
				Version ver2 = new Version(o2.replace(comp.getVcsRepository().getReleaseBranchPrefix(), ""));
				if (ver1.equals(ver2)) {
					return 0;
				}
				if (ver1.isGreaterThan(ver2)) {
					return -1;
				}
				return 1;
			}
		});
		
		
		ver = new Version(vcs.getFileContent(releaseBranches.get(0), SCMWorkflow.VER_FILE_NAME, null));
		List<VCSCommit> commits = vcs.getCommitsRange(releaseBranches.get(0), null, WalkDirection.DESC, 2);
		if (commits.size() == 2) {
			List<VCSTag> tags = vcs.getTagsOnRevision(commits.get(1).getRevision());
			if (!tags.isEmpty()) {
				for (VCSTag tag : tags) {
					if (tag.getTagName().equals(ver.toPreviousPatch().toReleaseString())) {
						// if db MODIFIED and head-1 commit of last release branch tagged then all is built and we need new release. Use DB version.
						// if db BRANCHED and head-1 commit of last release branch tagged then all is built and we need the built release. Use patch-- 
						// otherwise we must return last built RB version.
						DevelopBranchStatus dbs = new DevelopBranch(comp).getStatus();
						if (dbs == DevelopBranchStatus.BRANCHED) {
							ver = ver.toPreviousPatch();
						} else if(dbs == DevelopBranchStatus.MODIFIED) {
							ver = db.getVersion().toRelease(); 
						}
						break;
					}
				}
			}
		}
		this.version = ver;
		name = computeName();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comp == null) ? 0 : comp.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReleaseBranch other = (ReleaseBranch) obj;
		if (comp == null) {
			if (other.comp != null)
				return false;
		} else if (!comp.equals(other.comp))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	public ReleaseBranchStatus getStatus() {
		if (!exists()) {
			return ReleaseBranchStatus.MISSING;
		}

		if (mDepsFrozen()) {
			if (mDepsActual()) {
				if (isPreHeadCommitTaggedWithVersion() || isPreHeadCommitTagDelayed()) {
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
			mDepRB = new ReleaseBranch(mDep, mDep.getVersion(), repos);
			if (!mDepRB.isPreHeadCommitTaggedWithVersion() && !mDepRB.isPreHeadCommitTagDelayed()) {
				return false;
			}
			if (!mDep.getVersion().equals(mDepRB.getCurrentVersion().toPreviousPatch())) {
				return false;
			}
		}
		return true;
	}

	public boolean isPreHeadCommitTagDelayed() {
		CommitsFile cf = new CommitsFile();
		String delayedTagRevision = cf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
		if (delayedTagRevision == null) {
			return false;
		}
		
		List<VCSCommit> commits = vcs.getCommitsRange(getName(), null, WalkDirection.DESC, 2);
		if (commits.size() < 2) {
			return false;
		}
		
		return commits.get(1).getRevision().equals(delayedTagRevision);
	}

	public boolean isPreHeadCommitTaggedWithVersion() {
		if (!vcs.getBranches(comp.getVcsRepository().getReleaseBranchPrefix()).contains(getName())) {
			return false;
		}
		List<VCSCommit> commits = vcs.getCommitsRange(getName(), null, WalkDirection.DESC, 2);
		if (commits.size() < 2) {
			return false;
		}
		List<VCSTag> tags = vcs.getTagsOnRevision(commits.get(1).getRevision());
		if (tags.isEmpty()) {
			return false;
		}
		for (VCSTag tag : tags) {
			if (tag.getTagName().equals(getCurrentVersion().toPreviousPatch().toReleaseString())) {
				return true;
			}
		}
		return false;
	}

	public boolean exists() {
		String releaseBranchName = getName();
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
		return "ReleaseBranch [comp=" + comp + ", version=" + version.toReleaseString() + ", status=" + getStatus() + ", name=" + name + "]";
	}

	public List<Component> getMDeps() {
		if (!vcs.getBranches(comp.getVcsRepository().getReleaseBranchPrefix()).contains(name) || !vcs.fileExists(name, SCMWorkflow.MDEPS_FILE_NAME)) {
			return new ArrayList<>();
		}

		String mDepsFileContent = comp.getVCS().getFileContent(name, SCMWorkflow.MDEPS_FILE_NAME, null);
		MDepsFile mDeps = new MDepsFile(mDepsFileContent, repos);
		return mDeps.getMDeps();
	}

	public String getName() {
		return name;
	}

	public Version getCurrentVersion() {
		return new Version(comp.getVCS().getFileContent(getName(), SCMWorkflow.VER_FILE_NAME, null).trim());
	}

	public Version getVersion() {
		return version;
	}
}
