package org.scm4j.releaser.conf;

import org.scm4j.commons.Coords;
import org.scm4j.releaser.VCSFactory;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.builders.BuilderFactory;
import org.scm4j.releaser.exceptions.EComponentConfig;
import org.scm4j.releaser.exceptions.EConfig;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VCSRepositories {
	public static final String DEFAULT_VCS_WORKSPACE_DIR = new File(SCMReleaser.BASE_WORKING_DIR, "releaser-vcs-workspaces").getPath();

	private Map<?, ?> urls;
	private Map<?, ?> creds;
	private final IVCSWorkspace ws;

	private static IConfigSource configSource = new EnvVarsConfigSource();

	public static void setConfigSource(IConfigSource configSource) {
		VCSRepositories.configSource = configSource;
	}

	public VCSRepositories(String urlsStr, String credsStr) {
		this(urlsStr, credsStr, new VCSWorkspace());
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
		String devBranch = (String) getPropByName(urls, componentName, "devBranch", VCSRepository.DEFAULT_DEV_BRANCH);
		String releaseBranchPrefix = (String) getPropByName(urls, componentName, "releaseBranchPrefix",
				VCSRepository.DEFAULT_RELEASE_BRANCH_PREFIX);
		String builder = (String) getPropByName(urls, componentName, "builder", null);
		return new VCSRepository(componentName, url, credentials, type, devBranch, releaseBranchPrefix,
				VCSFactory.getVCS(type, credentials, url, ws), BuilderFactory.getBuilder(builder));
	}

	public VCSRepository getByCoords(String coords) {
		return getByName(new Coords(coords).getName());
	}

	private VCSType getVCSType(String type, String url) {
		if (type != null && type.toLowerCase().contains("git"))
			return VCSType.GIT;
		if (type != null && (type.toLowerCase().contains("svn") || type.toLowerCase().contains("subversion")))
			return VCSType.SVN;
		if (url != null && url.contains(".git")) {
			return VCSType.GIT;
		}
		return VCSType.GIT;
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
		try {
			URLContentLoader reposLoader = new URLContentLoader();

			String separatedReposUrlsStr = configSource.getReposLocations();
			if (separatedReposUrlsStr == null) {
				throw new EConfig(EnvVarsConfigSource.REPOS_LOCATION_ENV_VAR
						+ " environment var must contain a valid config path");
			}
			String reposContent = reposLoader.getContentFromUrls(separatedReposUrlsStr);
			String separatedCredsUrlsStr = configSource.getCredentialsLocations();
			if (separatedCredsUrlsStr == null) {
				throw new EConfig(EnvVarsConfigSource.CREDENTIALS_LOCATION_ENV_VAR
						+ " environment var must contain a valid config path");
			}
			String credsContent = reposLoader.getContentFromUrls(separatedCredsUrlsStr);
			try {
				return new VCSRepositories(reposContent, credsContent, new VCSWorkspace(DEFAULT_VCS_WORKSPACE_DIR));
			} catch (Exception e) {
				throw new EConfig(e);
			}
		} catch (IOException e) {
			throw new EConfig("Failed to read config", e);
		}
	}

	private static volatile VCSRepositories instance;

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