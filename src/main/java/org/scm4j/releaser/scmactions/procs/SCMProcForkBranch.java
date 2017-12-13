package org.scm4j.releaser.scmactions.procs;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.ActionTreeBuilder;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.vcs.api.IVCS;

public class SCMProcForkBranch implements ISCMProc {
	
	private final DevelopBranch db;
	private final Component comp;
	private final IVCS vcs;
	private final ExtendedStatus status;
	private final String newBranchName;
 
	public SCMProcForkBranch(Component comp, CachedStatuses cache) {
		db = new DevelopBranch(comp);
		vcs = comp.getVCS();
		status = cache.get(comp.getUrl());
		newBranchName = Utils.getReleaseBranchName(comp, status.getNextVersion());
		this.comp = comp;
	}
	
	@Override
	public void execute(IProgress progress) {
		createBranch(progress);
		
		truncateSnapshotReleaseVersion(progress);
		
		bumpTrunkMinorVersion(progress);
		
		//for what?
		//calculatedResult.replaceReleaseBranch(comp, new ReleaseBranch(comp, newTrunkVersion.toPreviousMinor().toReleaseZeroPatch(), true));
	}
	
	private void createBranch(IProgress progress) {
		Utils.reportDuration(() -> vcs.createBranch(comp.getVcsRepository().getDevelopBranch(), newBranchName, "release branch created"),
				"create branch " + newBranchName, null, progress);
	}
	
	private void truncateSnapshotReleaseVersion(IProgress progress) {
		String noSnapshotVersion = status.getNextVersion().toString();
		Utils.reportDuration(() -> vcs.setFileContent(newBranchName, ActionTreeBuilder.VER_FILE_NAME, noSnapshotVersion, LogTag.SCM_VER + " " + noSnapshotVersion),
				"truncate snapshot: " + noSnapshotVersion + " in branch " + newBranchName, null, progress);
	}
	
	private Version bumpTrunkMinorVersion(IProgress progress) {
		Version newMinorVersion = db.getVersion().toNextMinor();
		Utils.reportDuration(() -> vcs.setFileContent(comp.getVcsRepository().getDevelopBranch(), ActionTreeBuilder.VER_FILE_NAME, newMinorVersion.toString(), LogTag.SCM_VER + " " + newMinorVersion),
				"change to version " + newMinorVersion + " in trunk", null, progress);
		return newMinorVersion;
	}
}
