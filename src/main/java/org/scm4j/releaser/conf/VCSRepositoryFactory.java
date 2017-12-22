package org.scm4j.releaser.conf;

import org.scm4j.commons.RegexConfig;
import org.scm4j.releaser.VCSFactory;
import org.scm4j.releaser.builders.BuilderFactory;
import org.scm4j.releaser.exceptions.EComponentConfig;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;

public class VCSRepositoryFactory {
	
	public static final VCSType DEFAULT_VCS_TYPE = VCSType.GIT;
	
	private final RegexConfig repoConfig;
	private final RegexConfig credentialsConfig;
	private final IVCSWorkspace ws;
	
	public VCSRepositoryFactory(IConfig config) {
		this(config.getRepoConfig(), config.getCredentialsConfig(), config.getWS());
	}

	public VCSRepositoryFactory(RegexConfig repoConfig, RegexConfig credentialsConfig, IVCSWorkspace ws) {
		this.repoConfig = repoConfig;
		this.credentialsConfig = credentialsConfig;
		this.ws = ws;
	}
	
	public VCSRepository getVCSRepository(String componentName) {
		String url = repoConfig.getPlaceholderedStringByName(componentName, "url", null);
		if (url == null) {
			throw new EComponentConfig("no repo url for: " + componentName);
		}

		Credentials credentials;
		String user = credentialsConfig.getPropByName(url, "name", null);
		if (user != null) {
			String pass = credentialsConfig.getPropByName(url, "password", null);
			Boolean isDefault = credentialsConfig.getPropByName(url, "isDefault", false);
			credentials = new Credentials(user, pass, isDefault);
		} else {
			credentials = new Credentials(null, null, false);
		}
		VCSType type = getVCSType(repoConfig.getPropByName(componentName, "type", null), url);
		String developBranch = repoConfig.getPropByName(componentName, "developBranch", VCSRepository.DEFAULT_DEVELOP_BRANCH);
		String releaseBranchPrefix = repoConfig.getPropByName(componentName, "releaseBranchPrefix",
				VCSRepository.DEFAULT_RELEASE_BRANCH_PREFIX);
		String releaseCommand = repoConfig.getPropByName(componentName, "releaseCommand", null);
		return new VCSRepository(componentName, url, credentials, type, developBranch, releaseBranchPrefix,
				VCSFactory.getVCS(type, credentials, url, ws), BuilderFactory.getBuilder(releaseCommand));
	}
	
	private VCSType getVCSType(String type, String url) {
		if (type != null && type.toLowerCase().contains(VCSType.GIT.toString().toLowerCase()))
			return VCSType.GIT;
		if (type != null && (type.toLowerCase().contains(VCSType.SVN.toString().toLowerCase())
				|| type.toLowerCase().contains("subversion")))
			return VCSType.SVN;
		if (url != null && url.contains(VCSType.GIT.getUrlMark())) {
			return VCSType.GIT;
		}
		return DEFAULT_VCS_TYPE;
	}
}
