package org.scm4j.releaser.conf;

import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.VCSFactory;
import org.scm4j.releaser.builders.BuilderFactory;
import org.scm4j.releaser.exceptions.EComponentConfig;
import org.scm4j.releaser.exceptions.EConfig;
import org.scm4j.releaser.exceptions.EEnvVarConfig;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class VCSRepositories {
	public static final VCSType DEFAULT_VCS_TYPE = VCSType.GIT;
	public static final String DEFAULT_VCS_WORKSPACE_DIR = new File(SCMReleaser.BASE_WORKING_DIR,
			"releaser-vcs-workspaces").getPath();

	private static IConfigSource configSource = new EnvVarsConfigSource();

	private static volatile VCSRepositories instance;

	private Map<?, ?> urls;
	private Map<?, ?> creds;
	private final IVCSWorkspace ws;

	public static void setConfigSource(IConfigSource configSource) {
		VCSRepositories.configSource = configSource;
	}

	public VCSRepositories(String urlsStr, String credsStr) {
		this(urlsStr, credsStr, new VCSWorkspace(DEFAULT_VCS_WORKSPACE_DIR));
	}

	public VCSRepositories(String urlsStr, String credsStr, IVCSWorkspace ws) throws YAMLException {
		this.ws = ws;
		Yaml yaml = new Yaml();
		urls = (Map<?, ?>) yaml.load(urlsStr);
		if (urls == null) {
			urls = new HashMap<>();
		}
		creds = (Map<?, ?>) yaml.load(credsStr);
		if (creds == null) {
			creds = new HashMap<>();
		}
	}

	public VCSRepository getByName(String componentName) {
		String url = getPropByNameAsStringWithReplace(urls, componentName, "url", null);
		if (url == null) {
			throw new EComponentConfig("no repo url for: " + componentName);
		}

		Credentials credentials;
		String user = (String) getPropByName(creds, url, "name", null);
		if (user != null) {
			String pass = (String) getPropByName(creds, url, "password", null);
			Boolean isDefault = (Boolean) getPropByName(creds, url, "isDefault", false);
			credentials = new Credentials(user, pass, isDefault);
		} else {
			credentials = new Credentials(null, null, false);
		}
		VCSType type = getVCSType((String) getPropByName(urls, componentName, "type", null), url);
		String developBranch = (String) getPropByName(urls, componentName, "developBranch", VCSRepository.DEFAULT_DEVELOP_BRANCH);
		String releaseBranchPrefix = (String) getPropByName(urls, componentName, "releaseBranchPrefix",
				VCSRepository.DEFAULT_RELEASE_BRANCH_PREFIX);
		String releaseCommand = (String) getPropByName(urls, componentName, "releaseCommand", null);
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

	private Object getPropByName(Map<?, ?> map, String name, Object propName, Object defaultValue) {
		if (map != null) {
			for (Object key : map.keySet()) {
				if (key == null || name.matches((String) key)) {
					Map<?, ?> props = (Map<?, ?>) map.get(key);
					if (props.containsKey(propName))
						return props.get(propName);
				}
			}
		}
		return defaultValue;
	}

	private String getPropByNameAsStringWithReplace(Map<?, ?> map, String name, Object propName, String defaultValue) {
		String result = defaultValue;
		if (map != null) {
			for (Object key : map.keySet()) {
				if (key == null || name.matches((String) key)) {
					Map<?, ?> props = (Map<?, ?>) map.get(key);
					if (props.containsKey(propName)) {
						result = (String) props.get(propName);
						if (result != null)
							result = name.replaceFirst(key == null ? ".*" : (String) key, result);
						break;
					}
				}
			}
		}
		return result;
	}

	private static VCSRepositories loadVCSRepositories() throws EConfig {
		URLContentLoader reposLoader = new URLContentLoader();
		String separatedReposUrlsStr = configSource.getReposLocations();
		if (separatedReposUrlsStr == null) {
			throw new EEnvVarConfig(EnvVarsConfigSource.REPOS_LOCATION_ENV_VAR
					+ " environment var must contain a valid config path");
		}
		String separatedCredsUrlsStr = configSource.getCredentialsLocations();
		if (separatedCredsUrlsStr == null) {
			throw new EEnvVarConfig(EnvVarsConfigSource.CREDENTIALS_LOCATION_ENV_VAR
					+ " environment var must contain a valid config path");
		}
		try {
			String reposContent = reposLoader.getContentFromUrls(separatedReposUrlsStr);
			String credsContent = reposLoader.getContentFromUrls(separatedCredsUrlsStr);
			return new VCSRepositories(reposContent, credsContent, new VCSWorkspace(DEFAULT_VCS_WORKSPACE_DIR));
		} catch (Exception e) {
			throw new EConfig("Failed to read config", e);
		}
	}

	public static VCSRepositories getDefault() {
		VCSRepositories localInstance = instance;
		if (localInstance == null) {
			synchronized (VCSRepositories.class) {
				localInstance = instance;
				if (localInstance == null) {
					instance = localInstance = VCSRepositories.loadVCSRepositories();
				}
			}
		}
		return localInstance;
	}

	public static void resetDefault() {
		instance = null;
	}

}