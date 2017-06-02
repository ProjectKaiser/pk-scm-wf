package org.scm4j.wf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.scm4j.actions.ActionError;
import org.scm4j.actions.ActionNone;
import org.scm4j.actions.IAction;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.model.Dep;
import org.scm4j.wf.model.VCSRepository;

public class SCMWorkflow implements ISCMWorkflow {
	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String WORKSPACE_DIR = System.getProperty("java.io.tmpdir") + "scm4j-wf-workspaces";
	public static final String VER_FILE_NAME = "version";
	public static final String MDEPS_CHANGED_FILE_NAME = "mdeps-changed";
	private Map<String, VCSRepository> vcsRepos;

	public SCMWorkflow(Map<String, VCSRepository> vcsRepos) {
		this.vcsRepos = vcsRepos;
	}

	@Override
	public IAction calculateProductionReleaseAction(String depName) {
		String devBranchName = vcsRepos.get(depName).getDevBranch();
		List<IAction> childActions = new ArrayList<>();

		IVCS vcs = IVCSFactory.getIVCS(vcsRepos.get(depName));

		String mDepsContent = null;
		Boolean hasVer = vcs.fileExists(devBranchName, VER_FILE_NAME);

		Boolean processMDeps = vcs.fileExists(devBranchName, MDEPS_FILE_NAME);
		if (processMDeps) {
			mDepsContent = vcs.getFileContent(devBranchName, MDEPS_FILE_NAME);
		}

		List<Dep> mDeps;
		if (processMDeps) {
			mDeps = loadDeps(mDepsContent);
			for (Dep mDep : mDeps) {
				childActions.add(calculateProductionReleaseAction(mDep.getName()));
			}
		} else {
			mDeps = new ArrayList<>();
		}

		IAction res;
		BranchStructure struct = new BranchStructure(vcs, devBranchName);
		if (!hasVer) {
			res = new ActionError(vcsRepos.get(depName), childActions, devBranchName, "no " + VER_FILE_NAME + " file");
		} else if (hasErrorActions(childActions)) {
			res = new ActionNone(vcsRepos.get(depName), childActions, devBranchName);
		} else if (hasSignificantActions(childActions) || hasNewerDependencies(childActions, mDeps)) {
			res = new SCMActionProductionRelease(vcsRepos.get(depName), childActions, devBranchName,
					ProductionReleaseReason.NEW_DEPENDENCIES);
		} else if (struct.getHasFeatures()) {
			res = new SCMActionProductionRelease(vcsRepos.get(depName), childActions, devBranchName,
					ProductionReleaseReason.NEW_FEATURES);
		} else {
			res = new SCMActionUseLastReleaseVersion(vcsRepos.get(depName), childActions, devBranchName);
		}

		return res;
	}

	private boolean hasNewerDependencies(List<IAction> actions, List<Dep> mDeps) {
		for (IAction action : actions) {
			if (action instanceof SCMActionUseLastReleaseVersion) {
				SCMActionUseLastReleaseVersion verAction = (SCMActionUseLastReleaseVersion) action;
				// verAction = это использование существующей версии. посмотрим, а правильна ли версия соответствующего mDep
				for (Dep dep : mDeps) {
					if (dep.getName().equals(verAction.getName()) && (dep.getVersion().toString() == null || 
							!dep.getVersion().toString().equals(verAction.getVer().toString()))) {
						return true;
					}
				}
			}
		}
		return false;
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
			if (!(action instanceof ActionNone) && !(action instanceof SCMActionUseLastReleaseVersion)) {
				return true;
			}
		}
		return false;
	}

	private List<Dep> loadDeps(String mDepsContent) {
		return new MDepsFile(mDepsContent, vcsRepos).getMDeps();
	}

	@Override
	public void execActions(List<IAction> actions) {
	}

}
