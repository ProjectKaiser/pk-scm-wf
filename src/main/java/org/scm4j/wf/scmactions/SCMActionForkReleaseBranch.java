package org.scm4j.wf.scmactions;

import java.util.ArrayList;
import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.DevelopBranch;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.Option;
import org.scm4j.wf.conf.Version;

public class SCMActionForkReleaseBranch extends ActionAbstract {
	
	private final ReleaseReason reason;
	private final ReleaseBranch rb;
	private final DevelopBranch db;
	private final Version currentVer;
	private final IVCS vcs;

	public SCMActionForkReleaseBranch(Component comp, List<IAction> childActions, ReleaseReason reason, List<Option> options) {
		super(comp, childActions, options);
		this.reason = reason;
		rb = new ReleaseBranch(comp, repos);
		db = new DevelopBranch(comp);
		currentVer = db.getVersion();
		vcs = getVCS();
	}
	
	public ReleaseReason getReason() {
		return reason;
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
			ReleaseBranch rbCurrentMDep = new ReleaseBranch(currentMDep, repos);
			String futureReleaseVersionStr = rbCurrentMDep.getVersion().toReleaseString();
			if (new Version(futureReleaseVersionStr).isGreaterThan(currentMDep.getVersion())) {
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
			
			if (reason == ReleaseReason.ACTUALIZE_MDEPS) {
				raiseReleasePatch(progress);
			} else {
				raiseTrunkVersion(progress);
				raiseReleaseVersion(progress);
			}

		} catch (Throwable t) {
			progress.reportStatus("execution error: " + t.toString() + ": " + t.getMessage());
			throw new RuntimeException(t);
		}  
	}
	
	private void raiseReleasePatch(IProgress progress) {
		String newReleasePatchVersion = rb.getVersion().toNextPatch().toString();
		if (rb.getCurrentVersion().toString().equals(newReleasePatchVersion)) {
			progress.reportStatus("release branch version patch is changed already: " + newReleasePatchVersion);
			return;
		}
		getVCS().setFileContent(rb.getName(), SCMWorkflow.VER_FILE_NAME, newReleasePatchVersion, LogTag.SCM_VER + " " + newReleasePatchVersion);
		progress.reportStatus("change to patch version " + newReleasePatchVersion + " in branch " + rb.getName());
		
	}

	private void raiseReleaseVersion(IProgress progress) {
		String newReleaseVersion = currentVer.toReleaseString();
		if (rb.getCurrentVersion().toString().equals(newReleaseVersion)) {
			progress.reportStatus("release branch version is changed already: " + newReleaseVersion);
			return;
		}
		getVCS().setFileContent(rb.getName(), SCMWorkflow.VER_FILE_NAME, newReleaseVersion, LogTag.SCM_VER + " " + newReleaseVersion);
		progress.reportStatus("change to version " + newReleaseVersion + " in branch " + rb.getName());
	}

	private void raiseTrunkVersion(IProgress progress) {
		String newTrunkVersion = rb.getVersion().toNextMinor().toSnapshotString();
		if (db.getVersion().toString().equals(newTrunkVersion)) {
			progress.reportStatus("trunk version is raised already: " + newTrunkVersion);
			return;
		}
		getVCS().setFileContent(db.getName(), SCMWorkflow.VER_FILE_NAME, newTrunkVersion, LogTag.SCM_VER + " " + newTrunkVersion);
		progress.reportStatus("change to version " + newTrunkVersion + " in trunk");
	}

	@Override
	public String toString() {
		return "fork " + comp.getCoords().toString() + " " + rb.getName() + ", " + reason.toString();
	}
}
