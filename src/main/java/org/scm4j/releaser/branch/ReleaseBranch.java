package org.scm4j.releaser.branch;

import org.scm4j.commons.Version;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

import java.util.*;

public class ReleaseBranch {

	private final Component comp;
	private final Version version;
	private final IVCS vcs;
	private final String name;
	private Map<String, Object> cache = new HashMap<>();

	// comp version is ignored
	public ReleaseBranch(Component comp, Version version) {
		this.version = version.toRelease();
		this.comp = comp;
		name = computeName();
		vcs = comp.getVCS();
	}

	private String computeName() {
		return comp.getVcsRepository().getReleaseBranchPrefix() + version.getReleaseNoPatchString();
	}
	
	public ReleaseBranch(final Component comp) {
		if (comp.isProduct() && comp.getVersion().isExact()) {
			this.version = comp.getVersion().toRelease();
			this.comp = comp;
			name = computeName();
			vcs = comp.getVCS();
			return ;
		}
		this.comp = comp;
		vcs = comp.getVCS();
		DevelopBranch db = new DevelopBranch(comp);

		List<String> releaseBranches;
		if (comp.getVersion().isEmpty() || comp.getVersion().isSnapshot()) {
			releaseBranches = new ArrayList<>(vcs.getBranches(comp.getVcsRepository().getReleaseBranchPrefix()));
		} else {
			String exactReleaseBranchName = comp.getVcsRepository().getReleaseBranchPrefix() + comp.getVersion().getReleaseNoPatchString();
			if (vcs.getBranches(comp.getVcsRepository().getReleaseBranchPrefix()).contains(exactReleaseBranchName)) {
				releaseBranches = Arrays.asList(exactReleaseBranchName);
			} else {
				releaseBranches = new ArrayList<>();
			}
		}
		
		if (releaseBranches.isEmpty()) {
			this.version = db.getVersion().toRelease();
			name = computeName();
			return;
		}
		
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
		/**
		 * TODO: to test:
		 * We have api 4.1 released and have new commits in api dev. Execute status git:4.1. 
		 * We want to get api 4.1 ACTUAL since we provided exact git version but we get api 5.0 MISSING
		 */
		Version ver = new Version(vcs.getFileContent(releaseBranches.get(0), SCMReleaser.VER_FILE_NAME, null));
		List<VCSCommit> commits = vcs.getCommitsRange(releaseBranches.get(0), null, WalkDirection.DESC, 2);
		if (commits.size() == 2) {
			List<VCSTag> tags = vcs.getTagsOnRevision(commits.get(1).getRevision());
			if (!tags.isEmpty()) {
				for (VCSTag tag : tags) {
					if (tag.getTagName().equals(ver.toPreviousPatch().toReleaseString())) {
						DevelopBranchStatus dbs = db.getStatus();
						if(comp.getVersion().isExact()) {
							/**
							 * exact version provided and ACTUAL - result must be last ACTUAL despite the DevelopBranch.
							 */
							ver = ver.toPreviousPatch();
						} else if (dbs == DevelopBranchStatus.BRANCHED ) {
							/**
							 *   * - scm-ver 3.0-SNAPSHOT
							 *   |                         * - #scm-ver 2.1
							 *   |                         * - #scm-ver 2.0, tag
							 *   *------------------------/
							 *   result must be 2.0 ACTUAL
							 */
							ver = ver.toPreviousPatch();
						} else if (dbs == DevelopBranchStatus.MODIFIED) {
							/**
							 *   * - feature commit
							 *   * - scm-ver 3.0-SNAPSHOT
							 *   |                         * - #scm-ver 2.1
							 *   |                         * - #scm-ver 2.0, tag
							 *   *------------------------/
							 *   result must be 3.0 MISSING
							 */
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
		try {
			if (!exists()) {
				return ReleaseBranchStatus.MISSING;
			}
	
			if (mDepsFrozen()) {
				if (mDepsActual()) {
					if (isActual()) {
						return ReleaseBranchStatus.ACTUAL;
					}
					return ReleaseBranchStatus.MDEPS_ACTUAL;
				}
				return ReleaseBranchStatus.MDEPS_FROZEN;
			}
	
			return ReleaseBranchStatus.BRANCHED;
		} finally {
			cache.clear();
		}
	}

	private boolean isActual() {
		return isPreHeadCommitTaggedWithVersion() || isPreHeadCommitTagDelayed();
	}

	private boolean mDepsActual() {
		List<Component> mDeps = getMDeps();
		if (mDeps.isEmpty()) {
			return true;
		}
		ReleaseBranch mDepRB;
		for (Component mDep : mDeps) {
			mDepRB = new ReleaseBranch(mDep, mDep.getVersion());
			if (!mDepRB.isActual()) {
				return false;
			}
			if (!mDep.getVersion().equals(mDepRB.getHeadVersion().toPreviousPatch())) {
				return false;
			}
		}
		return true;
	}

	private boolean isPreHeadCommitTagDelayed() {
		DelayedTagsFile cf = new DelayedTagsFile();
		String delayedTagRevision = cf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
		if (delayedTagRevision == null) {
			return false;
		}

		List<VCSCommit> commits = getLast2Commits();
		return commits.size() >= 2 && commits.get(1).getRevision().equals(delayedTagRevision);
	}

	private boolean isPreHeadCommitTaggedWithVersion() {
		List<VCSCommit> commits = getLast2Commits();
		if (commits.size() < 2) {
			return false;
		}
		List<VCSTag> tags = vcs.getTagsOnRevision(commits.get(1).getRevision());
		if (tags.isEmpty()) {
			return false;
		}
		for (VCSTag tag : tags) {
			if (tag.getTagName().equals(getHeadVersion().toPreviousPatch().toReleaseString())) {
				return true;
			}
		}
		return false;
	}

	protected List<VCSCommit> getLast2Commits() {
		@SuppressWarnings("unchecked")
		List<VCSCommit> last2Commits = (List<VCSCommit>) cache.get("last2Commits");
		if (last2Commits == null) {
			last2Commits = vcs.getCommitsRange(getName(), null, WalkDirection.DESC, 2);
			cache.put("last2Commits", last2Commits);
		}
		return last2Commits;
	}

	public boolean exists() {
		return getBranches().contains(getName());
	}

	private Set<String> getBranches() {
		@SuppressWarnings("unchecked")
		Set<String> branches = (Set<String>) cache.get("branches");
		if (branches == null) {
			branches = vcs.getBranches(comp.getVcsRepository().getReleaseBranchPrefix());
			cache.put("branches", branches);
		}
		return branches;
	}

	private Boolean mDepsFrozen() {
		List<Component> mDeps = getMDeps();
		if (mDeps.isEmpty()) {
			return true;
		}
		for (Component mDep : mDeps) {
			if (mDep.getVersion().isSnapshot()) {
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
		try {
			String mDepsFileContent = comp.getVCS().getFileContent(name, SCMReleaser.MDEPS_FILE_NAME, null);
			MDepsFile mDeps = new MDepsFile(mDepsFileContent);
			return mDeps.getMDeps();
		} catch (EVCSFileNotFound e) {
			return new ArrayList<>();
		}
	}

	public String getName() {
		return name;
	}

	public Version getHeadVersion() {
		return new Version(comp.getVCS().getFileContent(getName(), SCMReleaser.VER_FILE_NAME, null).trim());
	}

	public Version getVersion() {
		return version;
	}
}
