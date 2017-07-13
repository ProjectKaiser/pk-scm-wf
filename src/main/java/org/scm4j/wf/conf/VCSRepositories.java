package org.scm4j.wf.conf;

import java.util.HashMap;
import java.util.Map;

import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;
import org.scm4j.wf.exceptions.EConfig;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public class VCSRepositories {
	private Map<?, ?> urls;
	private Map<?, ?> creds;
	private final IVCSWorkspace ws;
	
	public VCSRepositories(String urlsStr, String credsStr) {
		this(urlsStr, credsStr, new VCSWorkspace());
	}

	public VCSRepositories(String urlsStr, String credsStr, IVCSWorkspace ws) throws YAMLException {
		this.ws = ws;
		Yaml yaml = new Yaml();
		urls = yaml.loadAs(urlsStr, Map.class);
		if (urls == null) {
			urls = new HashMap<>();
		}
		creds = yaml.loadAs(credsStr, Map.class);
		if (creds == null) {
			creds = new HashMap<>();
		}
	}

	public VCSRepository get(String name) {
		String url = getPropByNameAsStringWithReplace(urls, name, "url", null);
		if (url == null) {
			throw new EConfig("no repo url for: " + name);
		}
		
		Credentials credentials; 
		if (url != null && getPropByName(creds, url, "name", null) != null) {
			String user = (String) getPropByName(creds, url, "name", null);
			String pass = (String) getPropByName(creds, url, "password", null);
			Boolean isDefault = (Boolean) getPropByName(creds, url, "isDefault", false);
			credentials = new Credentials(user, pass, isDefault);
		} else {
			credentials = new Credentials(null, null, false);
		}
		VCSType type = getVCSType((String) getPropByName(urls, name, "type", null), url);
		String devBranch = (String) getPropByName(urls, name, "devBranch", VCSRepository.DEFAULT_DEV_BRANCH);
		String releaseBranchPrefix = (String) getPropByName(urls, name, "releaseBanchPrefix", VCSRepository.DEFAULT_RELEASE_BRANCH_PREFIX);
		VCSRepository result = new VCSRepository(name, url, credentials, type, devBranch, ws, releaseBranchPrefix);
		return result;
	}

	private VCSType getVCSType(String type, String url) {
		if (type != null && type.toLowerCase().contains("git"))
			return VCSType.GIT;
		if (type != null && (type.toLowerCase().contains("svn") || type.toLowerCase().contains("subversion")))
			return VCSType.SVN;
		if (url != null && url.contains(".git")) {
			return VCSType.GIT;
		}
		return VCSType.SVN;
	}

	private Object getPropByName(Map<?, ?> map, String name, Object propName, Object defaultValue) {
		if (map != null) {
			for (Object key: map.keySet()) {
				if (name.matches((String) key)) {
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
			for (Object key: map.keySet()) {
				if (name.matches((String) key)) {
					Map<?, ?> props = (Map<?, ?>) map.get(key);
					if (props.containsKey(propName)) {
						result = (String) props.get(propName);
						if (result != null)
							result = name.replaceFirst((String) key, result);
						break;
					}
				}
			}
		}
		return result;
	}

}
