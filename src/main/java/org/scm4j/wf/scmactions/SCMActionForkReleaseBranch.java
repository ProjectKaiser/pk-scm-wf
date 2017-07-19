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
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.MDepsFile;

public class SCMActionForkReleaseBranch extends ActionAbstract {

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
			ActionResultReleaseBranchFork existingResult = (ActionResultReleaseBranchFork) getResult(getName(), ActionResultReleaseBranchFork.class);
			if (existingResult != null) {
				progress.reportStatus("release branch already forked: " + existingResult.getBranchName()); 
				return existingResult;
			}
			
			IVCS vcs = comp.getVcsRepository().getVcs();
			DevelopBranch db = new DevelopBranch(comp);
			
			// fork branch
			vcs.createBranch(db.getName(), db.getReleaseBranchName(), "release branch created");
			progress.reportStatus("branch " + db.getReleaseBranchName() + " created");
			
			// let's fix mdep versions
			List<Component> actualMDeps = db.getMDeps();
			List<Component> frozenMDeps = new ArrayList<>();
			for (Component actualMDep : actualMDeps) {
				DevelopBranch dbActualMDep = new DevelopBranch(actualMDep);
				String futureRelaseVersionStr = dbActualMDep.getVersion().toReleaseString();
				Component frozenMDep = actualMDep.cloneWithDifferentVersion(futureRelaseVersionStr);
				frozenMDeps.add(frozenMDep);
			}
			MDepsFile frozenMDepsFile = new MDepsFile(frozenMDeps);
			vcs.setFileContent(db.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, frozenMDepsFile.toFileContent(), LogTag.SCM_MDEPS);
			progress.reportStatus("mdeps frozen");
			
			ActionResultReleaseBranchFork res = new ActionResultReleaseBranchFork(db.getReleaseBranchName());
			return res;
		} catch (Throwable t) {
			progress.reportStatus("execution error: " + t.toString() + ": " + t.getMessage());
			return t;
		}  
	}

}
