package org.scm4j.wf;

import org.scm4j.actions.ActionError;
import org.scm4j.actions.ActionNone;
import org.scm4j.actions.IAction;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.model.Dep;
import org.scm4j.wf.model.VCSRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SCMWorkflow implements ISCMWorkflow {

	public static final String DEFAULT_VCS_WORKSPACE_DIR = new File(System.getProperty("user.home"), 
			".scm4j" + File.separator + "wf-vcs-workspaces").getPath();
	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String MDEPS_CHANGED_FILE_NAME = "mdeps-changed";
	private Map<String, VCSRepository> vcsRepos;
	private String depName;
	List<IAction> childActions = new ArrayList<>();
	private String devBranchName;
	private IVCS vcs;
	private List<Dep> mDeps = new ArrayList<>();
	private IVCSWorkspace ws;
	
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
	
	public SCMWorkflow(String depName, Map<String, VCSRepository> vcsRepos, IVCSWorkspace ws) {
		this.vcsRepos = vcsRepos;
		this.depName = depName;
		this.ws = ws;
		devBranchName = getRepoByName(depName).getDevBranch();
		vcs = VCSFactory.getIVCS(getRepoByName(depName), ws);
		if (vcs.fileExists(devBranchName, MDEPS_FILE_NAME)) {
			String mDepsContent = vcs.getFileContent(devBranchName, MDEPS_FILE_NAME);
			mDeps = new MDepsFile(mDepsContent, vcsRepos).getMDeps();
		} 
	}

	public SCMWorkflow(String depName) {
		this(depName, VCSRepository.loadFromEnvironment(), new VCSWorkspace(DEFAULT_VCS_WORKSPACE_DIR));
	}

	public SCMWorkflow(String depName, String configPath) {
		this(depName, VCSRepository.loadFromEnvironment(), new VCSWorkspace(configPath));
	}


	@Override
	public IAction getProductionReleaseAction() {
		for (Dep mDep : mDeps) {
			ISCMWorkflow childWorkflow = new SCMWorkflow(mDep.getName(), vcsRepos, ws);
			childActions.add(childWorkflow.getProductionReleaseAction());
		}
		return getAction();
	}

	public IAction getAction() {
		IAction res;
		Boolean hasVer = vcs.fileExists(devBranchName, VER_FILE_NAME);
		if (!hasVer) {
			res = new ActionError(getRepoByName(depName), childActions, devBranchName, "no " + VER_FILE_NAME + " file", ws);
		} else if (hasErrorActions(childActions)) {
			res = new ActionNone(getRepoByName(depName), childActions, devBranchName, ws);
		} else if (new BranchStructure(vcs, devBranchName).getHasFeatures()) {
			res = new SCMActionProductionRelease(getRepoByName(depName), childActions, devBranchName,
					ProductionReleaseReason.NEW_FEATURES, ws);
		} else if (hasSignificantActions(childActions) || hasNewerDependencies(childActions, mDeps)) {
			res = new SCMActionProductionRelease(getRepoByName(depName), childActions, devBranchName,
					ProductionReleaseReason.NEW_DEPENDENCIES, ws);
		}  else {
			res = new SCMActionUseLastReleaseVersion(getRepoByName(depName), childActions, devBranchName, ws);
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
