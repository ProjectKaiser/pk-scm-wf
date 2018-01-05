package org.scm4j.releaser.scmactions.procs;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSChangeListNode;

import java.util.List;

public class SCMProcForkBranch implements ISCMProc {
	
	private final DevelopBranch db;
	private final IVCS vcs;
	private final ExtendedStatus status;
	private final String newBranchName;
	private final VCSRepository repo;
	private final List<VCSChangeListNode> vcsChangeList;

	public SCMProcForkBranch(Component comp, CachedStatuses cache, VCSRepository repo, List<VCSChangeListNode> vcsChangeList) {
		db = new DevelopBranch(comp, repo);
		this.repo = repo;
		vcs = repo.getVCS();
		status = cache.get(repo.getUrl());
		newBranchName = Utils.getReleaseBranchName(repo, status.getNextVersion());
		this.vcsChangeList = vcsChangeList;

	}
	
	@Override
	public void execute(IProgress progress) {
		createBranch(progress);
		
		truncateSnapshotReleaseVersion(progress);
		
		bumpTrunkMinorVersion(progress);
	}
	
	private void createBranch(IProgress progress) {
		Utils.reportDuration(() -> vcs.createBranch(repo.getDevelopBranch(), newBranchName, "release branch created"),
				"create branch " + newBranchName, null, progress);
	}
	
	private void truncateSnapshotReleaseVersion(IProgress progress) {
		String noSnapshotVersion = status.getNextVersion().toString();
		vcsChangeList.add(new VCSChangeListNode(Utils.VER_FILE_NAME, noSnapshotVersion, LogTag.SCM_VER + " " + noSnapshotVersion));
	}
	
	private void bumpTrunkMinorVersion(IProgress progress) {
		Version newMinorVersion = db.getVersion().toNextMinor();
		Utils.reportDuration(() -> vcs.setFileContent(repo.getDevelopBranch(), Utils.VER_FILE_NAME, newMinorVersion.toString(), LogTag.SCM_VER + " " + newMinorVersion),
				"change to version " + newMinorVersion + " in trunk", null, progress);
	}
}
