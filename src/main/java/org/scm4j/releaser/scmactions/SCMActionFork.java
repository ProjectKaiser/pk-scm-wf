package org.scm4j.releaser.scmactions;

import java.util.ArrayList;
import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.Build;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;

public class SCMActionFork extends ActionAbstract {
	
	private final DevelopBranch db;
	private final BuildStatus mbs;
	private final IVCS vcs;
	private final Version targetVersion;

	public SCMActionFork(ReleaseBranch crb, List<IAction> childActions, BuildStatus mbs) {
		super(crb.getComponent(), childActions);
		db = new DevelopBranch(comp);
		this.mbs = mbs;
		vcs = getVCS();
		targetVersion = crb.getVersion().toNextMinor();
	}
	
	@Override
	public void execute(IProgress progress)  {
		if (isUrlProcessed(comp.getVcsRepository().getUrl())) {
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
				break;
			default:
				throw new IllegalStateException(mbs + " target action is occured when fork only is expected");
			}
			addProcessedUrl(comp.getVcsRepository().getUrl());
		} catch (EReleaserException e) {
			throw e;
		} catch (Exception e) {
			progress.error("execution error: " + e.toString());
			throw new EReleaserException(e);
		}
	}
	
	private void createBranch(IProgress progress) {
		String newBranchName =  ReleaseBranch.getName(comp, targetVersion);
		vcs.createBranch(db.getName(), newBranchName, "release branch created");
		progress.reportStatus("branch " + newBranchName + " created");
	}
	
	private void actualizeMDeps(IProgress progress) {
		ReleaseBranch crb = new ReleaseBranch(comp);
		List<Component> currentMDeps = crb.getMDeps();
		if (currentMDeps.isEmpty()) {
			progress.reportStatus("no mdeps");
			return;
		}
		List<Component> actualizedMDeps = new ArrayList<>();
		for (Component currentMDep : currentMDeps) {
			ReleaseBranch crbMDep;
			Version futureReleaseVersion;
			if (comp.getVersion().isExact()) {
				crbMDep = new ReleaseBranch(currentMDep, comp.getVersion());
				futureReleaseVersion = crbMDep.getHeadVersion();
			} else {
				crbMDep = new ReleaseBranch(currentMDep);
				Build mbMDep = new Build(currentMDep);
				futureReleaseVersion = crbMDep.getVersion();
				if (mbMDep.isNeedToFork()) {
					futureReleaseVersion = futureReleaseVersion.toNextMinor().toRelease();
				}
			}
			actualizedMDeps.add(currentMDep.cloneWithDifferentVersion(futureReleaseVersion.toString()));
		}
		MDepsFile actualizedMDepsFile = new MDepsFile(actualizedMDeps);
		vcs.setFileContent(crb.getName(), SCMReleaser.MDEPS_FILE_NAME, actualizedMDepsFile.toFileContent(), LogTag.SCM_MDEPS);
		progress.reportStatus("mdeps actualized");
	}

	private void truncateSnapshotReleaseVersion(IProgress progress) {
		String noSnapshotVersion = db.getVersion().toReleaseString();
		String newBranchName =  ReleaseBranch.getName(comp, targetVersion);
		getVCS().setFileContent(newBranchName, SCMReleaser.VER_FILE_NAME, noSnapshotVersion, LogTag.SCM_VER + " " + noSnapshotVersion);
		progress.reportStatus("truncated snapshot: " + noSnapshotVersion + " in branch " + newBranchName);
	}

	private void raiseTrunkMinorVersion(IProgress progress) {
		Version newMinorVersion = db.getVersion().toNextMinor();
		getVCS().setFileContent(db.getName(), SCMReleaser.VER_FILE_NAME, newMinorVersion.toString(), LogTag.SCM_VER + " " + newMinorVersion);
		progress.reportStatus("change to version " + newMinorVersion + " in trunk");
	}

	@Override
	public String toString() {
		return "fork " + comp.getCoords() + ", " + targetVersion;
	}
	
}
