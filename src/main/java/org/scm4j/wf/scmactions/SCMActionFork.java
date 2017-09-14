package org.scm4j.wf.scmactions;

import java.util.ArrayList;
import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.DevelopBranch;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.branch.ReleaseBranchStatus;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.Option;

public class SCMActionFork extends ActionAbstract {
	
	private final ReleaseBranchStatus fromStatus;
	private final ReleaseBranchStatus toStatus;
	private final ReleaseBranch rb;
	private final DevelopBranch db;
	private final IVCS vcs;

	public SCMActionFork(Component comp, List<IAction> childActions, ReleaseBranchStatus fromStatus, ReleaseBranchStatus toStatus, List<Option> options) {
		super(comp, childActions, options);
		this.toStatus = toStatus;
		this.fromStatus = fromStatus;
		rb = new ReleaseBranch(comp);
		db = new DevelopBranch(comp);
		vcs = getVCS();
	}
	
	@Override
	public void execute(IProgress progress) {
		try {
			for (IAction action : childActions) {
				try (IProgress nestedProgress = progress.createNestedProgress(action.toString())) {
					action.execute(nestedProgress);
				}
			}
			
			createBranch(progress);
			
			actualizeMDeps(progress);
			
			raiseTrunkMinorVersion(progress);
			truncateSnapshotReleaseVersion(progress);

		} catch (Throwable t) {
			progress.reportStatus("execution error: " + t.toString() + ": " + t.getMessage());
			throw new RuntimeException(t);
		}  
	}
	
	private void createBranch(IProgress progress) {
		if (rb.exists()) {
			progress.reportStatus("release branch already forked: " + rb.getName());
			return;
		}
		vcs.createBranch(db.getName(), rb.getName(), "release branch created");
		progress.reportStatus("branch " + rb.getName() + " created");
	}
	
	private void actualizeMDeps(IProgress progress) {
		List<Component> currentMDeps = rb.getMDeps();
		if (currentMDeps.isEmpty()) {
			progress.reportStatus("no mdeps");
			return;
		}
		List<Component> actualizedMDeps = new ArrayList<>();
		Boolean hasNewMDeps = false;
		for (Component currentMDep : currentMDeps) {
			if (currentMDep.getVersion().isSnapshot() || getFromStatus() == ReleaseBranchStatus.MDEPS_FROZEN) {
				// TODO: added test: create UBL release, create unTillDb patch and then new release and test UBL actualizes just patch, not new version.
				ReleaseBranch rbCurrentMDep = new ReleaseBranch(currentMDep);
				String futureReleaseVersionStr = rbCurrentMDep.getVersion().toReleaseString();
				actualizedMDeps.add(currentMDep.cloneWithDifferentVersion(futureReleaseVersionStr));
				hasNewMDeps = true;
			} else {
				actualizedMDeps.add(currentMDep);
			}
		}
		if (hasNewMDeps) {
			MDepsFile frozenMDepsFile = new MDepsFile(actualizedMDeps);
			vcs.setFileContent(rb.getName(), SCMWorkflow.MDEPS_FILE_NAME, frozenMDepsFile.toFileContent(), LogTag.SCM_MDEPS); 
			progress.reportStatus("mdeps actualized");
		} else {
			progress.reportStatus("no mdeps to actualize");
		}
	}
	
	
	
//	private void raiseReleasePatch(IProgress progress) {
//		String newReleasePatchVersion = rb.getVersion().toNextPatch().toString();
//		if (rb.getCurrentVersion().toString().equals(newReleasePatchVersion)) {
//			progress.reportStatus("release branch version patch is changed already: " + newReleasePatchVersion);
//			return;
//		}
//		getVCS().setFileContent(rb.getName(), SCMWorkflow.VER_FILE_NAME, newReleasePatchVersion, LogTag.SCM_VER + " " + newReleasePatchVersion);
//		progress.reportStatus("change to patch version " + newReleasePatchVersion + " in branch " + rb.getName());
//		
//	}

	private void truncateSnapshotReleaseVersion(IProgress progress) {
		Version rbCurVer = rb.getCurrentVersion();
		if (!rbCurVer.isSnapshot()) {
			progress.reportStatus("snapshot is truncated already in release branch: " + rbCurVer);
			return;
		}
		String noSnapshotVersion = rbCurVer.toReleaseString();
		getVCS().setFileContent(rb.getName(), SCMWorkflow.VER_FILE_NAME, noSnapshotVersion, LogTag.SCM_VER + " " + noSnapshotVersion);
		progress.reportStatus("truncated snapshot: " + noSnapshotVersion + " in branch " + rb.getName());
	}

	private void raiseTrunkMinorVersion(IProgress progress) {
		Version newMinorVersion = db.getVersion().setMinor(rb.getVersion().toNextMinor().getMinor());
		// TODO: add test: assume we have dev version 5.0-SNAPSHOT, released 3.2 patch -> do not commit 4.0-SNAPSHOT to trunk 
		if (db.getVersion().equals(newMinorVersion) || db.getVersion().isGreaterThan(newMinorVersion)) {
			progress.reportStatus("trunk version is raised already: " + newMinorVersion);
			return;
		}
		getVCS().setFileContent(db.getName(), SCMWorkflow.VER_FILE_NAME, newMinorVersion.toString(), LogTag.SCM_VER + " " + newMinorVersion);
		progress.reportStatus("change to version " + newMinorVersion + " in trunk");
	}

	@Override
	public String toString() {
		return "fork " + comp.getCoords() + ", " + rb.getVersion()+ " " + getFromStatus() + " -> " + getToStatus();
	}

	public ReleaseBranchStatus getFromStatus() {
		return fromStatus;
	}

	public ReleaseBranchStatus getToStatus() {
		return toStatus;
	}
}
