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
	public static final String VER_FILE_NAME = "version";
	public static final String MDEPS_CHANGED_FILE_NAME = "mdeps-changed";
	private Map<String, VCSRepository> vcsRepos;
	private String depName;
	List<IAction> childActions = new ArrayList<>();
	private String devBranchName;
	private IVCS vcs;
	private List<Dep> mDeps = new ArrayList<>();
	
	private VCSRepository getRepoByName(String name) {
		VCSRepository res = vcsRepos.get(name);
		if (res == null) {
			throw new IllegalArgumentException("VCSRepository is not found by name " +name);
		}
		return res;
	}
	
	public void setMDeps(List<Dep> mDeps) {
		this.mDeps = mDeps;
	}
	
	public void setChildActions(List<IAction> childActions) {
		this.childActions = childActions;
	}
	
	public SCMWorkflow(String depName, Map<String, VCSRepository> vcsRepos) {
		this.vcsRepos = vcsRepos;
		this.depName = depName;
		devBranchName = getRepoByName(depName).getDevBranch();
		vcs = VCSFactory.getIVCS(getRepoByName(depName));
		if (vcs.fileExists(devBranchName, MDEPS_FILE_NAME)) {
			String mDepsContent = vcs.getFileContent(devBranchName, MDEPS_FILE_NAME);
			mDeps = new MDepsFile(mDepsContent, vcsRepos).getMDeps();
		} 
	}

	@Override
	public IAction getProductionReleaseAction() {
		for (Dep mDep : mDeps) {
			ISCMWorkflow childWorkflow = new SCMWorkflow(mDep.getName(), vcsRepos);
			childActions.add(childWorkflow.getProductionReleaseAction());
		}
		return getAction();
	}

	public IAction getAction() {
		IAction res;
		Boolean hasVer = vcs.fileExists(devBranchName, VER_FILE_NAME);
		if (!hasVer) {
			res = new ActionError(getRepoByName(depName), childActions, devBranchName, "no " + VER_FILE_NAME + " file");
		} else if (hasErrorActions(childActions)) {
			res = new ActionNone(getRepoByName(depName), childActions, devBranchName);
		} else if (hasSignificantActions(childActions) || hasNewerDependencies(childActions, mDeps)) {
			res = new SCMActionProductionRelease(getRepoByName(depName), childActions, devBranchName,
					ProductionReleaseReason.NEW_DEPENDENCIES);
		} else if (new BranchStructure(vcs, devBranchName).getHasFeatures()) {
			res = new SCMActionProductionRelease(getRepoByName(depName), childActions, devBranchName,
					ProductionReleaseReason.NEW_FEATURES);
		} else {
			res = new SCMActionUseLastReleaseVersion(getRepoByName(depName), childActions, devBranchName);
		}

		return res;
	}

	private boolean hasNewerDependencies(List<IAction> actions, List<Dep> mDeps) {
		for (IAction action : actions) {
			if (action instanceof SCMActionUseLastReleaseVersion) {
				SCMActionUseLastReleaseVersion verAction = (SCMActionUseLastReleaseVersion) action;
				for (Dep dep : mDeps) {
					if (dep.getName().equals(verAction.getName()) && (dep.getVersion() == null || 
							!dep.getVersion().toReleaseString().equals(verAction.getVer().toPreviousMinorRelease()))) {
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

}
