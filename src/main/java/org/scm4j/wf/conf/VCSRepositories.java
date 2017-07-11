package org.scm4j.wf.conf;

import java.util.HashMap;
import java.util.Map;

import org.scm4j.wf.exceptions.EConfig;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public class VCSRepositories {
	private Map<?, ?> urls;
	private Map<?, ?> creds;

	public VCSRepositories(String urlsStr, String credsStr) throws YAMLException {
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
		VCSRepository result = new VCSRepository();

		result.setName(name);
		String url = getPropByNameAsStringWithReplace(urls, name, "url", result.getUrl());
		if (url == null) {
			throw new EConfig("no repo url for: " + name);
		}
		result.setUrl(url);
		Credentials credentials = new Credentials();
		result.setCredentials(credentials);
		if (result.getUrl() != null && getPropByName(creds, result.getUrl(), "name", null) != null) {
			credentials.setName((String) getPropByName(creds, result.getUrl(), "name", credentials.getName()));
			credentials.setPassword((String) getPropByName(creds, result.getUrl(), "password", credentials.getPassword()));
			credentials.setIsDefault((Boolean) getPropByName(creds, result.getUrl(), "isDefault", credentials.getIsDefault()));
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
