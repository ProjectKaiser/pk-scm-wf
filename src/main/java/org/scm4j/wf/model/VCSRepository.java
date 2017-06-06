package org.scm4j.wf.model;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.scm4j.vcs.api.workingcopy.IVCSRepositoryWorkspace;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.GsonUtils;

import com.google.gson.reflect.TypeToken;

public class VCSRepository {
	
	public static final String DEFAULT_RELEASE_BRANCH_PREFIX = "release/";

	private String name;
	private String url;
	private Credentials credentials;
	private VCSType type;
	private IVCSRepositoryWorkspace workspace;
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

	public IVCSRepositoryWorkspace getWorkspace() {
		return workspace;
	}

	public void setWorkspace(IVCSRepositoryWorkspace workspace) {
		this.workspace = workspace;
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
	
	public static List<VCSRepository> fromJson(String jsonStr, List<Credentials> credentials, IVCSWorkspace ws) {
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
    		repo.setWorkspace(ws.getVCSRepositoryWorkspace(repo.getUrl()));
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
	

}
