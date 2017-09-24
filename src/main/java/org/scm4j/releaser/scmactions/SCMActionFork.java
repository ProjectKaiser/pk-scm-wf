package org.scm4j.releaser.scmactions;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.*;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.IVCS;

import java.util.ArrayList;
import java.util.List;

public class SCMActionFork extends ActionAbstract {
	
	private final DevelopBranch db;
	private final MinorBuildStatus mbs;
	private final CurrentReleaseBranch crb;
	private final IVCS vcs;

	public SCMActionFork(CurrentReleaseBranch crb, List<IAction> childActions, MinorBuildStatus mbs) {
		super(crb.getComponent(), childActions);
		db = new DevelopBranch(comp);
		this.mbs = mbs;
		this.crb = crb;
		vcs = getVCS();
	}
	
	@Override
	public void execute(IProgress progress)  {
		if (isCompProcessed(comp)) {
			progress.reportStatus("already executed");
			return;
		}
		try {
			super.executeChilds(progress);
			switch(mbs) {
				case FORK:
					createBranch(progress);
					truncateSnapshotReleaseVersion(progress);
					raiseTrunkMinorVersion(progress);
				case FREEZE:
				case ACTUALIZE_PATCHES:
					actualizeMDeps(progress);
			}
		} catch (Exception e) {
			progress.reportStatus("execution error: " + e.toString() + ": " + e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	private void createBranch(IProgress progress) {
		vcs.createBranch(db.getName(), crb.getName(), "release branch created");
		progress.reportStatus("branch " + crb.getName() + " created");
	}
	
	private void actualizeMDeps(IProgress progress) {
		List<Component> currentMDeps = crb.getMDeps();
		if (currentMDeps.isEmpty()) {
			progress.reportStatus("no mdeps");
			return;
		}
		List<Component> actualizedMDeps = new ArrayList<>();
		for (Component currentMDep : currentMDeps) {
			CurrentReleaseBranch crbMDep = new CurrentReleaseBranch(currentMDep);
			String futureReleaseVersionStr = crbMDep.getVersion().toReleaseString();
			actualizedMDeps.add(currentMDep.cloneWithDifferentVersion(futureReleaseVersionStr));
			MDepsFile actualizedMDepsFile = new MDepsFile(actualizedMDeps);
			vcs.setFileContent(crb.getName(), SCMReleaser.MDEPS_FILE_NAME, actualizedMDepsFile.toFileContent(), LogTag.SCM_MDEPS);
			progress.reportStatus("mdeps actualized");
		}
	}

	private void truncateSnapshotReleaseVersion(IProgress progress) {
		String noSnapshotVersion = db.getVersion().setPatch("0").toReleaseString(); // TODO: test zeroing patch in release branch
		getVCS().setFileContent(crb.getName(), SCMReleaser.VER_FILE_NAME, noSnapshotVersion, LogTag.SCM_VER + " " + noSnapshotVersion);
		progress.reportStatus("truncated snapshot: " + noSnapshotVersion + " in branch " + crb.getName());
	}

	private void raiseTrunkMinorVersion(IProgress progress) {
		Version newMinorVersion = db.getVersion().toNextMinor();
		getVCS().setFileContent(db.getName(), SCMReleaser.VER_FILE_NAME, newMinorVersion.toString(), LogTag.SCM_VER + " " + newMinorVersion);
		progress.reportStatus("change to version " + newMinorVersion + " in trunk");
	}

	@Override
	public String toString() {
		return "fork " + comp.getCoords() + ", " + crb.getVersion();
	}

}
