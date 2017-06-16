package org.scm4j.wf;

import org.scm4j.actions.ActionAbstract;
import org.scm4j.actions.IAction;
import org.scm4j.actions.results.ActionResultVersion;
import org.scm4j.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.conf.ConfFile;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.model.Dep;
import org.scm4j.wf.model.VCSRepository;

import java.util.ArrayList;
import java.util.List;

public class SCMActionProductionRelease extends ActionAbstract {
	
	public static final String VCS_TAG_SCM_VER = "#scm-ver";
	public static final String VCS_TAG_SCM_MDEPS = "#scm-mdeps";
	public static final String VCS_TAG_SCM_IGNORE = "#scm-ignore";
	public static final String VCS_TAG_SCM_RELEASE = "#scm-ver release";
	public static final String[] VCS_TAGS = new String[] {VCS_TAG_SCM_VER, VCS_TAG_SCM_MDEPS, VCS_TAG_SCM_IGNORE};
	public static final String BRANCH_DEVELOP = "develop";
	public static final String BRANCH_RELEASE = "release";
	
	private ProductionReleaseReason reason;

	public SCMActionProductionRelease(VCSRepository repo, List<IAction> childActions, String masterBranchName, 
			ProductionReleaseReason reason, IVCSWorkspace ws) {
		super(repo, childActions, masterBranchName, ws);
		this.reason = reason;
	}

	public ProductionReleaseReason getReason() {
		return reason;
	}

	public void setReason(ProductionReleaseReason reason) {
		this.reason = reason;
	}

	@Override
	public Object execute(IProgress progress) {
		try {
			
			IVCS vcs = getVCS();
			Version currentVer = getDevVersion();
			progress.reportStatus("current trunk version: " + currentVer);
			
			Object nestedResult;
			for (IAction action : childActions) {
				try (IProgress nestedProgress = progress.createNestedProgress(action.getName())) {
					nestedResult = action.execute(nestedProgress);
					if (nestedResult instanceof Throwable) {
						return nestedResult;
					}
				}
				getExecutionResults().put(action.getName(), nestedResult);
			}
			
			// Are we built already?
			if (getExecutionResults().get(getName()) != null) {
				Object existingResult = getExecutionResults().get(getName());
				if (existingResult instanceof ActionResultVersion) {
					progress.reportStatus("using already built version " + ((ActionResultVersion) existingResult).getVersion()); 
					return existingResult;
				}
			}
			
			// We have a new versions map. Will write it to mdeps on the ground
			VCSCommit newVersionStartsFromCommit;
			List<String> mDepsChanged = new ArrayList<>();
			if (vcs.fileExists(currentBranchName, SCMWorkflow.MDEPS_FILE_NAME)) {
				String mDepsContent = vcs.getFileContent(currentBranchName, SCMWorkflow.MDEPS_FILE_NAME);
				MDepsFile mDepsFile = new MDepsFile(mDepsContent, repo);
				List<String> mDepsOut = new ArrayList<>();
				String mDepOut;
				for (Dep mDep : mDepsFile.getMDeps()) {
					nestedResult = getExecutionResults().get(mDep.getName());
					mDepOut = "";
					if (nestedResult != null && nestedResult instanceof ActionResultVersion) {
						ActionResultVersion res = (ActionResultVersion) nestedResult;
						if (res.getIsNewBuild()) {
							mDepOut = mDep.toString(res.getVersion());
						} else {
							if (!res.getVersion().equals(mDep.getVersion().toReleaseString())) {
								mDepOut = mDep.toString(res.getVersion());
							} 
						}
					} 
					if (mDepOut.isEmpty()) {
						mDepOut = mDep.toString();
					} else {
						mDepsChanged.add(mDepOut);
					}
					mDepsOut.add(mDepOut);
				}
				progress.reportStatus("new mdeps generated");
				
				String mDepsOutContent = ConfFile.toFileContent(mDepsOut);
				newVersionStartsFromCommit = vcs.setFileContent(currentBranchName, SCMWorkflow.MDEPS_FILE_NAME, 
						mDepsOutContent, VCS_TAG_SCM_MDEPS);
				if (newVersionStartsFromCommit == VCSCommit.EMPTY) {
					// ����������� �� ����������, �� ��� ��� ����� ���� ������� �����
					newVersionStartsFromCommit = vcs.getHeadCommit(currentBranchName);
					progress.reportStatus("mdeps file is not changed. Going to branch from " + newVersionStartsFromCommit);
				} else {
					progress.reportStatus("mdeps updated in trunk, revision " + newVersionStartsFromCommit);
				}
			} else {
				newVersionStartsFromCommit = vcs.getHeadCommit(currentBranchName);
				progress.reportStatus("no mdeps. Going to branch from head " + newVersionStartsFromCommit);
			}
			
			// ������� �����
			String newBranchName = repo.getReleaseBanchPrefix() + currentVer.toReleaseString(); 
			vcs.createBranch(currentBranchName, newBranchName, "branch created");
			progress.reportStatus("branch " + newBranchName + " created");
			
			// �������� minor ver � ������
			String verContent = currentVer.toNextMinorSnapshot();
			vcs.setFileContent(currentBranchName, SCMWorkflow.VER_FILE_NAME, 
					verContent, VCS_TAG_SCM_VER + " " + verContent);
			progress.reportStatus("change to version " + verContent + " in trunk");
			
			// �������� ver � �����
			verContent = currentVer.toReleaseString();
			vcs.setFileContent(newBranchName, SCMWorkflow.VER_FILE_NAME, verContent, 
					VCS_TAG_SCM_VER + " " + verContent);
			progress.reportStatus("change to version " + verContent + " in branch " + newBranchName);
			
			// ������� mdeps-changed
			if (!mDepsChanged.isEmpty()) {
				vcs.setFileContent(newBranchName, SCMWorkflow.MDEPS_CHANGED_FILE_NAME, ConfFile.toFileContent(mDepsChanged), 
						VCS_TAG_SCM_IGNORE);
				progress.reportStatus("mdeps-changed is written to branch " + newBranchName);
			}
			
			ActionResultVersion res = new ActionResultVersion(repo.getName(), currentVer.toReleaseString(), true);
			progress.reportStatus("new " + repo.getName() + " " 
					+ res.getVersion() + " is released in " + newBranchName);
			return res;
		} catch (Throwable t) {
			progress.reportStatus("execution error: " + t.toString() + ": " + t.getMessage());
			return t;
		} 
	}
	
	@Override
	public String toString() {
		return super.toString() + "; " + reason.toString();
	}
}
