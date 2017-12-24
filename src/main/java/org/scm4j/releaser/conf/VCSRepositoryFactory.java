package org.scm4j.releaser.conf;

import org.scm4j.commons.RegexConfig;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.VCSFactory;
import org.scm4j.releaser.builders.BuilderFactory;
import org.scm4j.releaser.exceptions.EComponentConfig;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;

import java.io.File;

public class VCSRepositoryFactory {
	
	public static final VCSType DEFAULT_VCS_TYPE = VCSType.GIT;
	public static final String DEFAULT_VCS_WORKSPACE_DIR = new File(Utils.BASE_WORKING_DIR,
			"releaser-vcs-workspaces").getPath();
	private final RegexConfig cc = new RegexConfig();
	private final RegexConfig creds = new RegexConfig();

	public VCSRepositoryFactory(IConfigUrls configUrls) {
		try {
			String ccUrls = configUrls.getCCUrls();
			if (ccUrls != null) {
				cc.loadFromYamlUrls(configUrls.getCCUrls());
			}
			String credsUrls = configUrls.getCredsUrl();
			if (credsUrls != null) {
				creds.loadFromYamlUrls(configUrls.getCredsUrl());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public VCSRepository getVCSRepository(String componentName) {
		String url = cc.getPlaceholderedStringByName(componentName, "url", null);
		if (url == null) {
			throw new EComponentConfig("no repo url for: " + componentName);
		}

		Credentials credentials;
		String user = creds.getPropByName(url, "name", null);
		if (user != null) {
			String pass = creds.getPropByName(url, "password", null);
			Boolean isDefault = creds.getPropByName(url, "isDefault", false);
			credentials = new Credentials(user, pass, isDefault);
		} else {
			credentials = new Credentials(null, null, false);
		}
		VCSType type = getVCSType(cc.getPropByName(componentName, "type", null), url);
		String developBranch = cc.getPropByName(componentName, "developBranch", VCSRepository.DEFAULT_DEVELOP_BRANCH);
		String releaseBranchPrefix = cc.getPropByName(componentName, "releaseBranchPrefix",
				VCSRepository.DEFAULT_RELEASE_BRANCH_PREFIX);
		String releaseCommand = cc.getPropByName(componentName, "releaseCommand", null);
		IVCSWorkspace ws = new VCSWorkspace(DEFAULT_VCS_WORKSPACE_DIR);
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
