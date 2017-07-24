package org.scm4j.wf.scmactions;

import java.util.ArrayList;
import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.results.ActionResultReleaseBranchFork;
import org.scm4j.wf.branchstatus.DevelopBranch;
import org.scm4j.wf.branchstatus.DevelopBranchStatus;
import org.scm4j.wf.branchstatus.ReleaseBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.conf.Version;

public class SCMActionForkReleaseBranch extends ActionAbstract {
	
	private final VCSRepositories repos = VCSRepositories.loadVCSRepositories();

	public SCMActionForkReleaseBranch(Component comp, List<IAction> childActions) {
		super(comp, childActions);
	}
	

	@Override
	public Object execute(IProgress progress) {
		try {
			Object nestedResult;
			for (IAction action : childActions) {
				try (IProgress nestedProgress = progress.createNestedProgress(action.getName())) {
					nestedResult = action.execute(nestedProgress);
					if (nestedResult instanceof Throwable) {
						return nestedResult;
					}
				}
				addResult(action.getName(), nestedResult);
			}
			
			// Are we forked already?
			ReleaseBranch rb = new ReleaseBranch(comp, repos);
			DevelopBranch db = new DevelopBranch(comp);
			Version currentVer = db.getVersion();
			IVCS vcs = comp.getVcsRepository().getVcs();
			if (rb.exists()) {
				progress.reportStatus("release branch already forked: " + rb.getReleaseBranchName());
			} else {
				vcs.createBranch(db.getName(), db.getReleaseBranchName(), "release branch created");
				progress.reportStatus("branch " + db.getReleaseBranchName() + " created");
			}
			String newBranchName = db.getReleaseBranchName();
			
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
						DevelopBranch dbActualMDep = new DevelopBranch(actualMDep);
						
						String futureRelaseVersionStr = dbActualMDep.getStatus() == DevelopBranchStatus.MODIFIED ? dbActualMDep.getVersion().toReleaseString() :
							dbActualMDep.getVersion().toPreviousMinorRelease();
								
						Component frozenMDep = actualMDep.cloneWithDifferentVersion(futureRelaseVersionStr);
						frozenMDeps.add(frozenMDep);
					}
					MDepsFile frozenMDepsFile = new MDepsFile(frozenMDeps);
					vcs.setFileContent(db.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, frozenMDepsFile.toFileContent(), LogTag.SCM_MDEPS);
					progress.reportStatus("mdeps frozen");
				}
			}
			
			String verContent = currentVer.toNextMinorSnapshot();
			vcs.setFileContent(db.getName(), SCMWorkflow.VER_FILE_NAME, verContent, LogTag.SCM_VER + " " + verContent);
			progress.reportStatus("change to version " + verContent + " in trunk");

			String newVersion = currentVer.toReleaseString();
			vcs.setFileContent(newBranchName, SCMWorkflow.VER_FILE_NAME, newVersion, LogTag.SCM_VER + " " + newVersion);
			progress.reportStatus("change to version " + newVersion + " in branch " + newBranchName);

			return new ActionResultReleaseBranchFork(db.getReleaseBranchName());
		} catch (Throwable t) {
			progress.reportStatus("execution error: " + t.toString() + ": " + t.getMessage());
			return t;
		}  
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
		return comp.getCoords().toString();
	}
}
