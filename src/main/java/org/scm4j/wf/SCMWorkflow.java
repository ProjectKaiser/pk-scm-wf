package org.scm4j.wf;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.scm4j.actions.ActionNone;
import org.scm4j.actions.IAction;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

import com.google.gson.reflect.TypeToken;

public class SCMWorkflow implements ISCMWorkflow {
	private static final String MDEPS_FILE_NAME = "mdeps.json";
	public static final String WORKSPACE_DIR = System.getProperty("java.io.tmpdir") + "scm4j-wf-workspaces";
	private static final String VER_FILE_NAME = "ver.json";
	private Map<String, Credentials> credentials;
	private Map<String, VCSRepository> vcsRepos;
	private Credentials defaultCred;
	private List<Dep> mDeps;
	
	public SCMWorkflow(Map<String, Credentials> credentials, Map<String, VCSRepository> vcsRepos) {
		this.credentials = credentials;
		this.vcsRepos = vcsRepos;
		for (Credentials cred : credentials.values()) {
			if (cred.getIsDefault()) {
				defaultCred = cred;
				break;
			}
		}
	}

	private Boolean isVersionBranch(String branchName) {
		return branchName.matches("^B[0-9]+$");
	}

	@Override
	public IAction calculateProductionReleaseAction(IVCS vcs, String masterBranchName) {
		List<IAction> childActions = new ArrayList<>();
		
		Boolean processMDeps;
		String mDepsContent = null;
		
		try {
			mDepsContent = vcs.getFileContent(masterBranchName, MDEPS_FILE_NAME);
			processMDeps = true;
		} catch (EVCSFileNotFound e) {
			processMDeps = false;
		}
		
		loadDeps(mDepsContent);
		
		for (Dep mDep : mDeps) {
			IVCS mDepVcs = IVCSFactory.getIVCS(vcs.getWorkspace(), mDep.getVcsRepository());
			childActions.add(calculateProductionReleaseAction(mDepVcs,  mDep.getVcsRepository().getMasterBranchName()));
		}

		String verContent = vcs.getFileContent(masterBranchName, VER_FILE_NAME);
		Dep currentDep = GsonUtils.fromJson(verContent, Dep.class);
		if (currentDep.getVcsRepository() == null) {
			currentDep.setVcsRepository(vcsRepos.get(currentDep.getName()));
		}
		IAction currentAction;
		List<VCSCommit> commits = vcs.getCommitsRange(masterBranchName, currentDep.getLastBuildCommitId(), null);
		if (commits.size() > 1) {
			currentAction = new SCMActionProductionRelease(null);
		} else {
			currentAction = new ActionNone(null);
		}
		
		IAction res;
		if (!childActions.isEmpty()) {
			res = new SCMActionProductionRelease(null);
			for (IAction childAction : childActions) {
				childAction.setParent(res);
			}
		} else {
			res = new ActionNone(null);
		}
		
		return res;
	}

	private void loadDeps(String mDepsContent) {
		Type token = new TypeToken<List<Dep>>() {}.getType();
		mDeps = GsonUtils.fromJson(mDepsContent, token);
    	
    	for (Dep mDep : mDeps) {
    		if (mDep.getVcsRepository() == null) {
    			mDep.setVcsRepository(vcsRepos.get(mDep.getName()));
    		}
    	}
	}

	

	@Override
	public void execActions(List<IAction> actions) {
		// TODO Auto-generated method stub

	}

}
