package org.scm4j.wf;

import org.scm4j.wf.Credentials;
import org.scm4j.wf.VCSType;

public class VCSRepository {

	private String name;
	private String url;
	private Credentials credentials;
	private VCSType type;

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

}
