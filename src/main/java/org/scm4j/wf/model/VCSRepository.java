package org.scm4j.wf.model;

import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import org.scm4j.wf.GsonUtils;
import org.scm4j.wf.exceptions.EWFNoConfig;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VCSRepository {
	
	public static final String DEFAULT_RELEASE_BRANCH_PREFIX = "release/";
	public static final String CONFIG_ENV_VAR = "SCM4J_VCS_REPOS";

	private String name;
	private String url;
	private Credentials credentials;
	private VCSType type;
	private String devBranch;
	private String releaseBanchPrefix = DEFAULT_RELEASE_BRANCH_PREFIX;
	
	public String getReleaseBanchPrefix() {
		return releaseBanchPrefix;
	}

	public void setReleaseBanchPrefix(String releaseBanchPrefix) {
		this.releaseBanchPrefix = releaseBanchPrefix;
	}

	public String getDevBranch() {
		return devBranch;
	}

	public void setDevBranch(String devBranch) {
		this.devBranch = devBranch;
	}

	public String getUrl() {
		return url;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Credentials getCredentials() {
		return credentials;
	}

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	public VCSType getType() {
		return type;
	}

	public void setType(VCSType type) {
		this.type = type;
	}

	public VCSRepository() {
	}

	@Override
	public String toString() {
		return "VCSRepository [url=" + url + "]";
	}
	
	public static List<VCSRepository> fromJson(String jsonStr, List<Credentials> credentials) {
		List<VCSRepository> res = new ArrayList<>();
		Type type = new TypeToken<List<VCSRepository>>() {}.getType();
    	List<VCSRepository> repos = GsonUtils.fromJson(jsonStr, type);
    	
    	Credentials defaultCred = null;
    	for (Credentials cred : credentials) {
    		if (cred.getIsDefault()) {
    			defaultCred = cred;
    			break;
    		}
    	}
    	
    	for (VCSRepository repo : repos) {
    		if (repo.getType() == null) {
    			repo.setType(getVCSType(repo.getUrl()));
    		}
    		if (repo.getCredentials() == null) {
    			repo.setCredentials(defaultCred);
    		} else {
    			repo.setCredentials(credentials.get(credentials.indexOf(repo.getCredentials())));
    		}
    		res.add(repo);
    	}
		return res;
	}
	
	private static VCSType getVCSType(String url) {
		if (url.contains(".git")) {
			return VCSType.GIT;
		}
		return VCSType.SVN;
	}

	public static Map<String, VCSRepository> loadFromEnvironment() {
		Map<String, VCSRepository> res = new HashMap<>();
		String storeUrlsStr = System.getenv(CONFIG_ENV_VAR);
		if (storeUrlsStr == null) {
			throw new EWFNoConfig(CONFIG_ENV_VAR + " environment var must contain valid config path");
		}
		try {
			Map<String, Credentials> creds = Credentials.loadFromEnvironment();
			String[] storeUrls = storeUrlsStr.split(";");
			for (String storeUrl : storeUrls) {
				URL url = new URL(storeUrl);
				String vcsReposJson;
				try (InputStream inputStream = url.openStream()) {
					vcsReposJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
				}
				List<VCSRepository> repos = VCSRepository.fromJson(vcsReposJson, new ArrayList<>(creds.values()));
				for (VCSRepository repo : repos) {
					res.put(repo.getName(), repo);
				}
			}
			return res;
		} catch (MalformedURLException e) {
			throw new EWFNoConfig(CONFIG_ENV_VAR + " environment var must contain valid config path");
		} catch (Exception e) {
			throw new EWFNoConfig("Failed to read config");
		}
	}
	

}
