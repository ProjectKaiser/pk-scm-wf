package org.scm4j.wf.model;

import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class VCSRepositories {
	private Map<?, ?> urls;
	private Map<?, ?> creds;

	public VCSRepositories(String urlsStr, String credsStr) {
		Yaml yaml = new Yaml();
		urls = (Map<?, ?>) yaml.load(urlsStr);
		creds = (Map<?, ?>) yaml.load(credsStr);
	}

	public VCSRepository get(String name) {
		VCSRepository result = new VCSRepository();

		result.setName(name);
		result.setUrl(getPropByNameAsStringWithReplace(urls, name, "url", result.getUrl()));
		if (result.getUrl() != null) {
			Credentials credentials = new Credentials();
			credentials.setName((String) getPropByName(creds, result.getUrl(), "name", credentials.getName()));
			credentials.setPassword((String) getPropByName(creds, result.getUrl(), "password", credentials.getPassword()));
			credentials.setIsDefault((Boolean) getPropByName(creds, result.getUrl(), "isDefault", credentials.getIsDefault()));
			result.setCredentials(credentials);
		}
		result.setType(getVCSType((String) getPropByName(urls, name, "type", null), result.getUrl()));
		result.setDevBranch((String) getPropByName(urls, name, "devBranch", result.getDevBranch()));
		result.setReleaseBanchPrefix((String) getPropByName(urls, name, "releaseBanchPrefix", result.getReleaseBanchPrefix()));

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
			for (Object key: map.keySet()) {
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

}
