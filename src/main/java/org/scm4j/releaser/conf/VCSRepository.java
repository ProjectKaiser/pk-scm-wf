package org.scm4j.releaser.conf;

import org.scm4j.releaser.builders.IBuilder;
import org.scm4j.vcs.api.IVCS;

public class VCSRepository {
	
	public static final String DEFAULT_RELEASE_BRANCH_PREFIX = "release/";
	public static final String DEFAULT_DEVELOP_BRANCH = null;
	
	private final String name;
	private final String url;
	private final Credentials credentials;
	private final VCSType type;
	private final String developBranch;
	private final String releaseBranchPrefix;
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
		return releaseBranchPrefix;
	}

	public String getDevelopBranch() {
		return developBranch;
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
						 VCSType type, String developBranch, String releaseBranchPrefix, IVCS vcs, IBuilder builder) {
		this.name = name;
		this.url = url;
		this.credentials = credentials;
		this.type = type;
		this.developBranch = developBranch;
		this.vcs = vcs;
		this.releaseBranchPrefix = releaseBranchPrefix;
		this.builder = builder;
	}

	@Override
	public String toString() {
		return "VCSRepository [url=" + url + "]";
	}

	public IVCS getVCS() {
		return vcs;
	}
	
	public IBuilder getBuilder() {
		return builder;
	}
	
}