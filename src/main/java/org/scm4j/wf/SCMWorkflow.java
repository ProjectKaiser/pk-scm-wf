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
import org.scm4j.wf.exceptions.EDepConfig;

public class SCMWorkflow implements ISCMWorkflow {

	public static final String REPOS_LOCATION_ENV_VAR = "SCM4J_VCS_REPOS";
	public static final String CREDENTIALS_LOCATION_ENV_VAR = "SCM4J_CREDENTIALS";
	public static final String DEFAULT_VCS_WORKSPACE_DIR = new File(System.getProperty("user.home"),
			".scm4j" + File.separator + "wf-vcs-workspaces").getPath();
	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String MDEPS_CHANGED_FILE_NAME = "mdeps-changed";
	private VCSRepositories repos;
	private Dep dep;
	private String devBranchName;
	private IVCS vcs;
	private List<Dep> mDeps = new ArrayList<>();
	private IVCSWorkspace ws;

	public void setMDeps(List<Dep> mDeps) {
		this.mDeps = mDeps;
	}
	
	public VCSRepository getRepoByName(String depName) {
		VCSRepository res = repos.get(depName);
		if (res == null) {
			throw new IllegalArgumentException("no repo url by name: " + depName);
		}
		return res;
	}

	public SCMWorkflow(String depCoords, VCSRepositories repos, IVCSWorkspace ws) {
		this(new Dep(depCoords, repos), repos, ws);
	}
	
	public SCMWorkflow(Dep dep, VCSRepositories repos, IVCSWorkspace ws) {
		this.repos = repos;
		this.dep = dep;
		this.ws = ws;
		devBranchName = dep.getVcsRepository().getDevBranch();
		vcs = VCSFactory.getIVCS(dep.getVcsRepository(), ws);
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

	@Override
	public IAction getProductionReleaseAction(List<IAction> childActions) {
		if (childActions == null) {
			childActions = new ArrayList<>();
		}
		
		for (Dep mDep : mDeps) {
			ISCMWorkflow childWorkflow = new SCMWorkflow(mDep, repos, ws);
			childActions.add(childWorkflow.getProductionReleaseAction(null));
		}
		return getProductionReleaseActionRoot(childActions);
	}

	public IAction getProductionReleaseActionRoot(List<IAction> childActions) {
		IAction res;
		Boolean hasVer = vcs.fileExists(devBranchName, VER_FILE_NAME);
		if (!hasVer) {
			throw new EDepConfig("no " + VER_FILE_NAME + " file for " + dep.toString());
//			res = new ActionNone(dep, childActions, devBranchName, ws,
//					"no " + VER_FILE_NAME + " file");
		} else if (hasErrorActions(childActions)) {
			res = new ActionNone(dep, childActions, devBranchName, ws, "has child error actions");
		} else if (new BranchStructure(vcs, devBranchName).getHasFeatures()) {
			res = new SCMActionProductionRelease(dep, childActions, devBranchName,
					ProductionReleaseReason.NEW_FEATURES, ws);
		} else if (hasSignificantActions(childActions) || hasNewerDependencies(childActions, mDeps)) {
			res = new SCMActionProductionRelease(dep, childActions, devBranchName,
					ProductionReleaseReason.NEW_DEPENDENCIES, ws);
		} else {
			res = new SCMActionUseLastReleaseVersion(dep, childActions, devBranchName, ws);
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
			ISCMWorkflow childWorkflow = new SCMWorkflow(mDep, repos, ws);
			childActions.add(childWorkflow.getTagReleaseAction(null));
		}
		return getTagReleaseActionRoot(childActions);
	}

	private IAction getTagReleaseActionRoot(List<IAction> childActions) {
		BranchStructure br = new BranchStructure(vcs, devBranchName);
		if (br.getReleaseTag() != null) {
			return new SCMActionUseExistingTag(dep, childActions, devBranchName, ws,
					br.getReleaseTag());
		} else {
			return new SCMActionTagRelease(dep, childActions, devBranchName, ws, "tag message");
		}
	}
}
