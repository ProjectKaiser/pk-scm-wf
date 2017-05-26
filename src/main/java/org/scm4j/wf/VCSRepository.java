package org.scm4j.wf;

import org.scm4j.vcs.api.workingcopy.IVCSRepositoryWorkspace;
import org.scm4j.wf.Credentials;
import org.scm4j.wf.VCSType;

public class VCSRepository {

	private String name;
	private String url;
	private Credentials credentials;
	private VCSType type;
	private IVCSRepositoryWorkspace workspace;

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

}
