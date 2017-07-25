package org.scm4j.wf.scmactions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSLockedWorkingCopy;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.results.ActionResultVersion;
import org.scm4j.wf.branchstatus.ReleaseBranch;
import org.scm4j.wf.branchstatus.ReleaseBranchStatus;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.conf.Version;

	
public class SCMActionBuild extends ActionAbstract {
	private final ProductionReleaseReason reason;
	private final Version targetVersion;

	public SCMActionBuild(Component comp, List<IAction> childActions, ProductionReleaseReason reason, Version targetVersion) {
		super(comp, childActions);
		this.reason = reason;
		this.targetVersion = targetVersion;
	}

	public ProductionReleaseReason getReason() {
		return reason;
	}

	@Override
	public Object execute(IProgress progress) {
		try {
			
			IVCS vcs = getVCS();
			VCSRepositories repos = VCSRepositories.loadVCSRepositories();
			ReleaseBranch rb = new ReleaseBranch(comp, targetVersion, repos);
			if (rb.getStatus() == ReleaseBranchStatus.BUILT) {
				progress.reportStatus("version " + rb.getVersion().toString() + " already built");
				return new ActionResultVersion(comp.getName(), rb.getVersion().toString(), true, rb.getReleaseBranchName());
			}
			
			progress.reportStatus("target version to build: " + targetVersion);
			
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
			
//			// Are we built already?
//			ActionResultVersion existingResult = (ActionResultVersion) getResult(getName(), ActionResultVersion.class);
//			if (existingResult != null) {
//				progress.reportStatus("using already built version " + existingResult.getVersion());
//				return existingResult;
//			}
			
			// We have a new versions map. Will write it to mdeps on the ground
//			ActionResultVersion existingResult;
//			VCSCommit newVersionStartsFromCommit;
//			List<String> mDepsChanged = new ArrayList<>();
//			if (vcs.fileExists(devBranch.getName(), SCMWorkflow.MDEPS_FILE_NAME)) {
//				String mDepsContent = vcs.getFileContent(devBranch.getName(), SCMWorkflow.MDEPS_FILE_NAME);
//				MDepsFile mDepsFile = new MDepsFile(mDepsContent, comp.getVcsRepository());
//				List<String> mDepsOut = new ArrayList<>();
//				String mDepOut;
//				for (Component mDep : mDepsFile.getMDeps()) {
//					existingResult = (ActionResultVersion) getResult(mDep.getName(), ActionResultVersion.class);
//					mDepOut = "";
//					if (existingResult != null) {
//						if (existingResult.getIsNewBuild()) {
//							mDepOut = mDep.getCoords().toString(existingResult.getVersion());
//						} else {
//							if (!existingResult.getVersion().equals(mDep.getCoords().getVersion().toReleaseString())) {
//								mDepOut = mDep.getCoords().toString(existingResult.getVersion());
//							} 
//						}
//					} 
//					if (mDepOut.isEmpty()) {
//						mDepOut = mDep.toString();
//					} else {
//						mDepsChanged.add(mDepOut);
//					}
//					mDepsOut.add(mDepOut);
//				}
//				progress.reportStatus("new mdeps generated");
//				
//				String mDepsOutContent = Utils.stringsToString(mDepsOut);
//				newVersionStartsFromCommit = vcs.setFileContent(devBranch.getName(), SCMWorkflow.MDEPS_FILE_NAME, 
//						mDepsOutContent, LogTag.SCM_MDEPS);
//				if (newVersionStartsFromCommit == VCSCommit.EMPTY) {
//					newVersionStartsFromCommit = vcs.getHeadCommit(devBranch.getName());
//					progress.reportStatus("mdeps file is not changed. Going to branch from " + newVersionStartsFromCommit);
//				} else {
//					progress.reportStatus("mdeps updated in trunk, revision " + newVersionStartsFromCommit);
//				}
//			} else {
//				newVersionStartsFromCommit = vcs.getHeadCommit(devBranch.getName());
//				progress.reportStatus("no mdeps. Going to branch from head " + newVersionStartsFromCommit);
//			}

			
			
//			if (!mDepsChanged.isEmpty()) {
//				vcs.setFileContent(newBranchName, SCMWorkflow.MDEPS_CHANGED_FILE_NAME, Utils.stringsToString(mDepsChanged), 
//						LogTag.SCM_IGNORE);
//				progress.reportStatus("mdeps-changed is written to branch " + newBranchName);
//			}
			
			if (comp.getVcsRepository().getBuilder() == null) {
				progress.reportStatus("builder is undefined");
			} else {
				try (IVCSLockedWorkingCopy lwc = vcs.getWorkspace().getVCSRepositoryWorkspace(vcs.getRepoUrl()).getVCSLockedWorkingCopy()) {
					lwc.setCorrupted(true); // use lwc only once
					vcs.checkout(rb.getReleaseBranchName(), lwc.getFolder().getPath());
					comp.getVcsRepository().getBuilder().build(comp, lwc.getFolder());
				}
			}
			
			/**
			 * теперь поставим теги
			 */
			
			
			vcs.createTag(rb.getReleaseBranchName(), targetVersion.toReleaseString(), "build tagged");
			
			ActionResultVersion res = new ActionResultVersion(comp.getName(), targetVersion.toReleaseString(), true,
					rb.getReleaseBranchName());
			progress.reportStatus(comp.getName() + " " + res.getVersion() + " is built in " + rb.getReleaseBranchName());
			//if (parentAction == null) {
				addResult(getName(), res); 
			//}
			return res;
		} catch (Throwable t) {
			progress.reportStatus("execution error: " + t.toString() + ": " + t.getMessage());
			return t;
		} 
	}

	@Override
	public String toString() {
		return comp.getCoords().toString() + ": " + reason.toString();
	}

}
