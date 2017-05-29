package org.scm4j.wf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.scm4j.actions.ActionError;
import org.scm4j.actions.ActionNone;
import org.scm4j.actions.IAction;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

public class SCMWorkflow implements ISCMWorkflow {
	public static final String MDEPS_FILE_NAME = "mdeps.conf";
	public static final String WORKSPACE_DIR = System.getProperty("java.io.tmpdir") + "scm4j-wf-workspaces";
	public static final String VER_FILE_NAME = "ver.conf";
	private Map<String, VCSRepository> vcsRepos;

	public SCMWorkflow(Map<String, VCSRepository> vcsRepos) {
		this.vcsRepos = vcsRepos;
	}

	@Override
	public IAction calculateProductionReleaseAction(String masterBranchName, String depName) {
		List<IAction> childActions = new ArrayList<>();
		
		IVCS vcs = IVCSFactory.getIVCS(vcsRepos.get(depName));
		
		String mDepsContent = null;
		String verContent;
		VerFile verFile = null;
		Boolean hasVer;
		try {
			verContent = vcs.getFileContent(masterBranchName, VER_FILE_NAME);
			verFile = VerFile.fromFileContent(verContent);
			hasVer = true;
		} catch (EVCSFileNotFound e) {
			hasVer = false;
		}
		
		Boolean processMDeps;
		try {
			mDepsContent = vcs.getFileContent(masterBranchName, MDEPS_FILE_NAME);
			processMDeps = true;
		} catch (EVCSFileNotFound e) {
			processMDeps = false;
		}
		
		if (processMDeps) {
			List<Dep> deps = loadDeps(mDepsContent);
			for (Dep mDep : deps) {
				childActions.add(calculateProductionReleaseAction(mDep.getMasterBranchName(), mDep.getName()));
			}
		}

		List<VCSCommit> commits;
		if (hasVer) {
			commits = vcs.getCommitsRange(masterBranchName, verFile.getLastVerCommit(), null);
		} else {
			commits = new ArrayList<>();
		}
		
		IAction res;
		if (!hasVer) {
			res = new ActionError(vcsRepos.get(depName), childActions, masterBranchName, 
					"no " + VER_FILE_NAME + " file");
		} else if (hasErrorActions(childActions)) {
			res = new ActionNone(vcsRepos.get(depName), childActions, masterBranchName);
		} else if (hasSignificantActions(childActions) || commits.size() > 2) {
			res = new SCMActionProductionRelease(vcsRepos.get(depName), childActions, masterBranchName);
		} else {
			res = new SCMActionUseExistingVersion(vcsRepos.get(depName), childActions, masterBranchName);
		}
		
		return res;
	}

	private boolean hasErrorActions(List<IAction> actions) {
		for (IAction action : actions) {
			if (action instanceof ActionError) {
				return true;
			}
		}
		return false;
	}

	private boolean hasSignificantActions(List<IAction> actions) {
		for (IAction action : actions) {
			if (!(action instanceof ActionNone) && !(action instanceof SCMActionUseExistingVersion)) {
				return true;
			}
		}
		return false;
	}

	private List<Dep> loadDeps(String mDepsContent) {
		List<String> strs = MDepsFile.fromFileContent(mDepsContent);
		List<Dep> deps = new ArrayList<>();
		for (String str : strs) {
			Dep dep = new Dep();
			String[] parts = str.split(":");
			dep.setName(str.replace(":" + parts[2], ""));
			dep.setVer(parts[2]);
			dep.setVcsRepository(vcsRepos.get(dep.getName()));
			deps.add(dep);
		}
		return deps;
	}

	@Override
	public void execActions(List<IAction> actions) {
	}

}
