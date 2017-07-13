package org.scm4j.wf.conf;

import org.scm4j.vcs.api.workingcopy.IVCSRepositoryWorkspace;

public class VCSRepository {
	
	public static final String DEFAULT_RELEASE_BRANCH_PREFIX = "release/";
	
	private String name;
	private String url;
	private Credentials credentials;
	private VCSType type;
	private String devBranch;
	private String releaseBanchPrefix = DEFAULT_RELEASE_BRANCH_PREFIX;
	private final IVCSRepositoryWorkspace ws;
	
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

	public VCSRepository(IVCSRepositoryWorkspace ws) {
		this.ws = ws;
	}

	@Override
	public String toString() {
		return "VCSRepository [url=" + url + "]";
	}

	public IVCSRepositoryWorkspace getWorkspace() {
		return ws;
	}

}
