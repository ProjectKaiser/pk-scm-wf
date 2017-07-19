package org.scm4j.wf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;
import org.scm4j.wf.actions.ActionError;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branchstatus.DevelopBranch;
import org.scm4j.wf.branchstatus.DevelopBranchStatus;
import org.scm4j.wf.branchstatus.ReleaseBranch;
import org.scm4j.wf.branchstatus.ReleaseBranchStatus;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.EnvVarsConfigSource;
import org.scm4j.wf.conf.IConfigSource;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.URLContentLoader;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.conf.VCSRepository;
import org.scm4j.wf.exceptions.EConfig;
import org.scm4j.wf.exceptions.EComponentConfig;
import org.scm4j.wf.scmactions.ProductionReleaseReason;
import org.scm4j.wf.scmactions.SCMActionForkReleaseBranch;
import org.scm4j.wf.scmactions.SCMActionProductionRelease;
import org.scm4j.wf.scmactions.SCMActionTagRelease;
import org.scm4j.wf.scmactions.SCMActionUseExistingTag;
import org.scm4j.wf.scmactions.SCMActionUseLastReleaseVersion;

public class SCMWorkflow implements ISCMWorkflow {


	public static final String DEFAULT_VCS_WORKSPACE_DIR = new File(System.getProperty("user.home"),
			".scm4j" + File.separator + "wf-vcs-workspaces").getPath();
	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String MDEPS_CHANGED_FILE_NAME = "mdeps-changed";
	private static IConfigSource configSource = new EnvVarsConfigSource();
	private static IVCSFactory vcsFactory = new VCSFactory();
	private VCSRepositories repos;
	private Component comp;
	private IVCS vcs;
	private List<Component> mDeps = new ArrayList<>();
	
	public void setMDeps(List<Component> mDeps) {
		this.mDeps = mDeps;
	}
	
	public static void setVCSFactory(IVCSFactory vcsFactory) {
		SCMWorkflow.vcsFactory = vcsFactory;
	}

	public static void setConfigSource(IConfigSource configSource) {
		SCMWorkflow.configSource = configSource;
	}
	
	public VCSRepository getRepoByName(String depName) {
		VCSRepository res = repos.getByComponent(depName);
		if (res == null) {
			throw new IllegalArgumentException("no repo url by name: " + depName);
		}
		return res;
	}

	public SCMWorkflow(String coords, VCSRepositories repos) {
		this(new Component(coords, repos), repos);
	}
	
	public SCMWorkflow(Component comp, VCSRepositories repos) {
		this.repos = repos;
		this.comp = comp;
		String devBranchName = comp.getVcsRepository().getDevBranch();
		vcs = comp.getVcsRepository().getVcs(); //VCSFactory.getVCS(dep.getVcsRepository(), ws);
		if (vcs.fileExists(devBranchName, MDEPS_FILE_NAME)) {
			String mDepsContent = vcs.getFileContent(devBranchName, MDEPS_FILE_NAME);
			mDeps = new MDepsFile(mDepsContent, repos).getMDeps();
		}
	}
	
	public SCMWorkflow(String coords) throws EConfig {
		this(coords, loadVCSRepositories());
	}
	
	public static VCSRepositories loadVCSRepositories() throws EConfig {
		try {
			URLContentLoader reposLoader = new URLContentLoader();
			
			String separatedReposUrlsStr = configSource.getReposLocations();
			if (separatedReposUrlsStr == null) {
				throw new EConfig(EnvVarsConfigSource.REPOS_LOCATION_ENV_VAR +
						" environment var must contain a valid config path");
			}
			String reposContent = reposLoader.getContentFromUrls(separatedReposUrlsStr);
			String separatedCredsUrlsStr = configSource.getCredentialsLocations();
			if (separatedCredsUrlsStr == null) {
				throw new EConfig(EnvVarsConfigSource.CREDENTIALS_LOCATION_ENV_VAR +
						" environment var must contain a valid config path");
			}
			String credsContent = reposLoader.getContentFromUrls(separatedCredsUrlsStr);
			try {
				return new VCSRepositories(reposContent, credsContent, new VCSWorkspace(DEFAULT_VCS_WORKSPACE_DIR), vcsFactory);
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
		
		for (Component mDep : mDeps) {
			ISCMWorkflow childWorkflow = new SCMWorkflow(mDep, repos);
			childActions.add(childWorkflow.getProductionReleaseAction(null));
		}
		return getProductionReleaseActionRoot(childActions);
	}

	public IAction getProductionReleaseActionRoot(List<IAction> childActions) {
		DevelopBranch db  = new DevelopBranch(comp);
		if (!db.hasVersionFile()) {
			throw new EComponentConfig("no " + VER_FILE_NAME + " file for " + comp.toString());
		}
		
		if (hasErrorActions(childActions)) {
			return new ActionNone(comp, childActions, "has child error actions");
		}
		if (db.getStatus() == DevelopBranchStatus.MODIFIED) {
			ReleaseBranch rb = new ReleaseBranch(comp, repos);
			if (!rb.exists()) {
				return new SCMActionForkReleaseBranch(comp, childActions);
			}
			return new SCMActionProductionRelease(comp, childActions, ProductionReleaseReason.NEW_FEATURES);
		}
		if (hasSignificantActions(childActions) || hasNewerDependencies(childActions, mDeps)) {
			ReleaseBranch rb = new ReleaseBranch(comp, repos);
			if (!rb.exists()) {
				return new SCMActionForkReleaseBranch(comp, childActions);
			}
			return new SCMActionProductionRelease(comp, childActions, ProductionReleaseReason.NEW_DEPENDENCIES);
		}
		return new SCMActionUseLastReleaseVersion(comp, childActions);
	}

	private boolean hasNewerDependencies(List<IAction> actions, List<Component> mDeps) {
		for (IAction action : actions) {
			if (action instanceof SCMActionUseLastReleaseVersion) {
				SCMActionUseLastReleaseVersion verAction = (SCMActionUseLastReleaseVersion) action;
				for (Component comp : mDeps) {
					if (comp.getCoords().getName().equals(verAction.getName()) && (comp.getVersion() == null || !comp.getVersion()
							.toPreviousMinorRelease().equals(verAction.getVersion().toPreviousMinorRelease()))) {
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
		for (Component mDep : mDeps) {
			ISCMWorkflow childWorkflow = new SCMWorkflow(mDep, repos);
			childActions.add(childWorkflow.getTagReleaseAction(null));
		}
		return getTagReleaseActionRoot(childActions);
	}

	private IAction getTagReleaseActionRoot(List<IAction> childActions) {
		ReleaseBranch rb = new ReleaseBranch(comp, repos);
		if (rb.getStatus() == ReleaseBranchStatus.TAGGED) {
			return new SCMActionUseExistingTag(comp, childActions, rb.getReleaseTag());
		} else {
			return new SCMActionTagRelease(comp, childActions, "tag message");
		}
	}
}
