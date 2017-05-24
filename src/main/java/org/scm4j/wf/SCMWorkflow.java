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
	private Map<String, VCSRepository> vcsRepos;
	private List<Dep> mDeps;
	
	public SCMWorkflow(Map<String, VCSRepository> vcsRepos) {
		this.vcsRepos = vcsRepos;
	}

	@Override
	public IAction calculateProductionReleaseAction(IVCS vcs, String masterBranchName) {
		List<IAction> childActions = new ArrayList<>();
		
		String mDepsContent = null;
		
		String verContent = vcs.getFileContent(masterBranchName, VER_FILE_NAME);
		Dep currentDep = GsonUtils.fromJson(verContent, Dep.class);
		if (currentDep.getVcsRepository() == null) {
			currentDep.setVcsRepository(vcsRepos.get(currentDep.getName()));
		}
		
		Boolean processMDeps;
		try {
			mDepsContent = vcs.getFileContent(masterBranchName, MDEPS_FILE_NAME);
			processMDeps = true;
		} catch (EVCSFileNotFound e) {
			processMDeps = false;
		}
		
		if (processMDeps) {
			loadDeps(mDepsContent);
			
			for (Dep mDep : mDeps) {
				IVCS mDepVcs = IVCSFactory.getIVCS(vcs.getWorkspace(), mDep.getVcsRepository());
				childActions.add(calculateProductionReleaseAction(mDepVcs, mDep.getMasterBranchName()));
			}
		}

		List<VCSCommit> commits = vcs.getCommitsRange(masterBranchName, currentDep.getLastBuildCommitId(), null);
		
		IAction res;
		if (!childActions.isEmpty() || commits.size() > 1) {
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
