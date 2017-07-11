package org.scm4j.wf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;
import org.scm4j.wf.actions.ActionError;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.conf.Dep;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.URLContentLoader;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.conf.VCSRepository;
import org.scm4j.wf.exceptions.EConfig;

public class SCMWorkflow implements ISCMWorkflow {

	public static final String REPOS_LOCATION_ENV_VAR = "SCM4J_VCS_REPOS";
	public static final String CREDENTIALS_LOCATION_ENV_VAR = "SCM4J_CREDENTIALS";
	public static final String DEFAULT_VCS_WORKSPACE_DIR = new File(System.getProperty("user.home"),
			".scm4j" + File.separator + "wf-vcs-workspaces").getPath();
	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String MDEPS_CHANGED_FILE_NAME = "mdeps-changed";
	private VCSRepositories repos;
	private String depName;
	private String devBranchName;
	private IVCS vcs;
	private List<Dep> mDeps = new ArrayList<>();
	private IVCSWorkspace ws;

	private VCSRepository getRepoByName(String name) {
		VCSRepository res = repos.get(name);
		if (res == null) {
			throw new IllegalArgumentException("VCSRepository is not found by name " + name);
		}
		return res;
	}

	public void setMDeps(List<Dep> mDeps) {
		this.mDeps = mDeps;
	}

	public SCMWorkflow(String depName, VCSRepositories repos, IVCSWorkspace ws) {
		this.repos = repos;
		this.depName = depName;
		this.ws = ws;
		devBranchName = getRepoByName(depName).getDevBranch();
		vcs = VCSFactory.getIVCS(getRepoByName(depName), ws);
		if (vcs.fileExists(devBranchName, MDEPS_FILE_NAME)) {
			String mDepsContent = vcs.getFileContent(devBranchName, MDEPS_FILE_NAME);
			mDeps = new MDepsFile(mDepsContent, repos).getMDeps();
		}
	}

	public SCMWorkflow(String depName) throws EConfig {
		this(depName, getReposFromEnvironment(), new VCSWorkspace(DEFAULT_VCS_WORKSPACE_DIR));
	}

	private static VCSRepositories getReposFromEnvironment() throws EConfig {
		try {
			URLContentLoader reposLoader = new URLContentLoader();
			String separatedReposUrlsStr = System.getenv(REPOS_LOCATION_ENV_VAR);
			if (separatedReposUrlsStr == null) {
				throw new EConfig(REPOS_LOCATION_ENV_VAR + " environment var must contain a valid config path");
			}
			String reposContent = reposLoader.getContentFromUrls(separatedReposUrlsStr);
			String separatedCredsUrlsStr = System.getenv(CREDENTIALS_LOCATION_ENV_VAR);
			if (separatedCredsUrlsStr == null) {
				throw new EConfig(CREDENTIALS_LOCATION_ENV_VAR + " environment var must contain a valid config path");
			}
			String credsContent = reposLoader.getContentFromUrls(separatedCredsUrlsStr);
			try {
				return new VCSRepositories(reposContent, credsContent);
			} catch (Exception e) {
				throw new EConfig(e);
			}
		} catch (IOException e) {
			throw new EConfig("Failed to read config", e);
		}
	}

	public SCMWorkflow(String depName, VCSRepositories repos, String configPath) {
		this(depName, repos, new VCSWorkspace(configPath));
	}

	@Override
	public IAction getProductionReleaseAction(List<IAction> childActions) {
		if (childActions == null) {
			childActions = new ArrayList<>();
		}
		
		for (Dep mDep : mDeps) {
			ISCMWorkflow childWorkflow = new SCMWorkflow(mDep.getName(), repos, ws);
			childActions.add(childWorkflow.getProductionReleaseAction(null));
		}
		return getProductionReleaseOneAction(childActions);
	}

	public IAction getProductionReleaseOneAction(List<IAction> childActions) {
		IAction res;
		Boolean hasVer = vcs.fileExists(devBranchName, VER_FILE_NAME);
		if (!hasVer) {
			res = new ActionNone(getRepoByName(depName), childActions, devBranchName, ws,
					"no " + VER_FILE_NAME + " file");
		} else if (hasErrorActions(childActions)) {
			res = new ActionNone(getRepoByName(depName), childActions, devBranchName, ws, "has child error actions");
		} else if (new BranchStructure(vcs, devBranchName).getHasFeatures()) {
			res = new SCMActionProductionRelease(getRepoByName(depName), childActions, devBranchName,
					ProductionReleaseReason.NEW_FEATURES, ws);
		} else if (hasSignificantActions(childActions) || hasNewerDependencies(childActions, mDeps)) {
			res = new SCMActionProductionRelease(getRepoByName(depName), childActions, devBranchName,
					ProductionReleaseReason.NEW_DEPENDENCIES, ws);
		} else {
			res = new SCMActionUseLastReleaseVersion(getRepoByName(depName), childActions, devBranchName, ws);
		}
		return res;
	}

	private boolean hasNewerDependencies(List<IAction> actions, List<Dep> mDeps) {
		for (IAction action : actions) {
			if (action instanceof SCMActionUseLastReleaseVersion) {
				SCMActionUseLastReleaseVersion verAction = (SCMActionUseLastReleaseVersion) action;
				for (Dep dep : mDeps) {
					if (dep.getName().equals(verAction.getName()) && (dep.getVersion() == null || !dep.getVersion()
							.toPreviousMinorRelease().equals(verAction.getVer().toPreviousMinorRelease()))) {
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

	@Override
	public IAction getTagReleaseAction(List<IAction> childActions) {
		if (childActions == null) {
			childActions = new ArrayList<>();
		}
		for (Dep mDep : mDeps) {
			ISCMWorkflow childWorkflow = new SCMWorkflow(mDep.getName(), repos, ws);
			childActions.add(childWorkflow.getTagReleaseAction(null));
		}
		return getTagReleaseOneAction(childActions);
	}

	private IAction getTagReleaseOneAction(List<IAction> childActions) {
		BranchStructure br = new BranchStructure(vcs, devBranchName);
		if (br.getReleaseTag() != null) {
			return new SCMActionUseExistingTag(getRepoByName(depName), childActions, devBranchName, ws,
					br.getReleaseTag());
		} else {
			return new SCMActionTagRelease(getRepoByName(depName), childActions, devBranchName, ws, "tag message");
		}
	}
}
