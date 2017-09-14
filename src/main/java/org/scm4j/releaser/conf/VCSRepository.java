package org.scm4j.releaser.conf;

import org.scm4j.releaser.builders.IBuilder;
import org.scm4j.vcs.api.IVCS;

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
	private final IBuilder builder;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VCSRepository other = (VCSRepository) obj;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

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
						 VCSType type, String devBranch, String releaseBranchPrefix, IVCS vcs, IBuilder builder) {
		this.name = name;
		this.url = url;
		this.credentials = credentials;
		this.type = type;
		this.devBranch = devBranch;
		this.vcs = vcs;
		this.releaseBanchPrefix = releaseBranchPrefix;
		this.builder = builder;
	}

	@Override
	public String toString() {
		return "VCSRepository [url=" + url + "]";
	}

	public IVCS getVcs() {
		return vcs;
	}
	
	public IBuilder getBuilder() {
		return builder;
	}
	
}