package org.scm4j.releaser.scmactions.procs;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.vcs.api.IVCS;

public class SCMProcForkBranch implements ISCMProc {
	
	private final ReleaseBranch rb;
	private final DevelopBranch db;
	private final IVCS vcs;
 
	public SCMProcForkBranch(ReleaseBranch rb) {
		this.rb = rb;
		db = new DevelopBranch(rb.getComponent());
		vcs = rb.getComponent().getVCS();
	}
	
	@Override
	public void execute(IProgress progress) {
		createBranch(progress);
		
		truncateSnapshotReleaseVersion(progress);
		
		bumpTrunkMinorVersion(progress);
	}
	
	private void createBranch(IProgress progress) {
		String newBranchName = rb.getName();
		progress.startTrace("Creating branch " + newBranchName + "... ");
		vcs.createBranch(db.getName(), newBranchName, "release branch created");
		progress.endTrace("done");
	}
	
	private void truncateSnapshotReleaseVersion(IProgress progress) {
		String noSnapshotVersion = rb.getVersion().toString();
		String newBranchName = rb.getName();
		progress.startTrace("truncating snapshot: " + noSnapshotVersion + " in branch " + newBranchName + "... ");
		vcs.setFileContent(newBranchName, SCMReleaser.VER_FILE_NAME, noSnapshotVersion, LogTag.SCM_VER + " " + noSnapshotVersion);
		progress.endTrace("done");
	}
	
	private void bumpTrunkMinorVersion(IProgress progress) {
		Version newMinorVersion = db.getVersion().toNextMinor();
		progress.startTrace("changing to version " + newMinorVersion + " in trunk... ");
		vcs.setFileContent(db.getName(), SCMReleaser.VER_FILE_NAME, newMinorVersion.toString(), LogTag.SCM_VER + " " + newMinorVersion);
		progress.endTrace("done");
	}
}
