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
	private ReleaseBranch rb;
	private DevelopBranch db;
	private Version currentVer;
	private IVCS vcs;

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
		vcs.createBranch(db.getName(), rb.getReleaseBranchName(), "release branch created");
		progress.reportStatus("branch " + rb.getReleaseBranchName() + " created");
	}
	
	private void freezeMDeps(IProgress progress) {
		// let's fix mdep versions
		List<Component> actualMDeps = db.getMDeps();
		if (actualMDeps.isEmpty()) {
			progress.reportStatus("no mdeps");
		} else {
			if (mDepsFrozen()) {
				progress.reportStatus("mdeps are frozen already");
			} else {
				List<Component> frozenMDeps = new ArrayList<>();
				for (Component actualMDep : actualMDeps) {
					ReleaseBranch rbActualMDep = new ReleaseBranch(actualMDep, repos);
					String futureRelaseVersionStr = rbActualMDep.getVersion().toReleaseString();
					Component frozenMDep = actualMDep.cloneWithDifferentVersion(futureRelaseVersionStr);
					frozenMDeps.add(frozenMDep);
				}
				MDepsFile frozenMDepsFile = new MDepsFile(frozenMDeps);
				vcs.setFileContent(rb.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, frozenMDepsFile.toFileContent(), LogTag.SCM_MDEPS);
				progress.reportStatus("mdeps frozen");
			}
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
			
			if (rb.exists()) {
				progress.reportStatus("release branch already forked: " + rb.getReleaseBranchName());
				return;
			}
			
			createBranch(progress);
			
			freezeMDeps(progress);
			
			raiseTrunkVersion(progress);
			
			raiseReleaseVersion(progress);

		} catch (Throwable t) {
			progress.reportStatus("execution error: " + t.toString() + ": " + t.getMessage());
			throw new RuntimeException(t);
		}  
	}
	
	private void raiseReleaseVersion(IProgress progress) {
		String newReleaseVersion = currentVer.toReleaseString();
		getVCS().setFileContent(rb.getReleaseBranchName(), /*newBranchName*/ SCMWorkflow.VER_FILE_NAME, newReleaseVersion, LogTag.SCM_VER + " " + newReleaseVersion);
		progress.reportStatus("change to version " + newReleaseVersion + " in branch " + rb.getReleaseBranchName());
	}

	private void raiseTrunkVersion(IProgress progress) {
		String newTrunkVersion = db.getVersion().toNextMinor().toString();
		getVCS().setFileContent(db.getName(), SCMWorkflow.VER_FILE_NAME, newTrunkVersion, LogTag.SCM_VER + " " + newTrunkVersion);
		progress.reportStatus("change to version " + newTrunkVersion + " in trunk");
	}

	private boolean mDepsFrozen() {
		ReleaseBranch rb = new ReleaseBranch(comp, repos);
		List<Component> mDeps = rb.getMDeps();
		if (mDeps.isEmpty()) {
			return false;
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
		return "fork " + comp.getCoords().toString() + " " + rb.getReleaseBranchName() + ", " + reason.toString();
	}
}
