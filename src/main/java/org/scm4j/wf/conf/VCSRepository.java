package org.scm4j.wf.conf;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;

public class VCSRepository {
	
	public static final String DEFAULT_RELEASE_BRANCH_PREFIX = "release/";
	public static final String DEFAULT_DEV_BRANCH = null;
	
	private final String name;
	private final String url;
	private final Credentials credentials;
	private final VCSType type;
	private final String devBranch;
	private final String releaseBanchPrefix;
	private final IVCS vcs;
	
	public String getReleaseBranchPrefix() {
		return releaseBanchPrefix;
	}

	public String getDevBranch() {
		return devBranch;
	}
	
	public String getUrl() {
		return url;
	}

	public String getName() {
		return name;
	}

	public Credentials getCredentials() {
		return credentials;
	}


	public VCSType getType() {
		return type;
	}
	
	public VCSRepository(String name, String url, Credentials credentials,
						 VCSType type, String devBranch, IVCSWorkspace ws, String releaseBranchPrefix, IVCS vcs) {
		this.name = name;
		this.url = url;
		this.credentials = credentials;
		this.type = type;
		this.devBranch = devBranch;
		this.vcs = vcs;
		this.releaseBanchPrefix = releaseBranchPrefix;
	}

	public VCSRepository(String name, String url, Credentials credentials, VCSType type, String devBranch,
						 IVCSWorkspace ws, IVCS vcs) {
		this(name, url, credentials, type, devBranch, ws, DEFAULT_RELEASE_BRANCH_PREFIX, vcs);
	}

	@Override
	public String toString() {
		return "VCSRepository [url=" + url + "]";
	}

	public IVCS getVcs() {
		return vcs;
	}
	
}