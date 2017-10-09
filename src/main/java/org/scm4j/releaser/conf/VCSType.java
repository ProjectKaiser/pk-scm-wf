package org.scm4j.releaser.conf;

public enum VCSType {
	GIT(".git"), SVN("");
	
	final String urlMark;
	
	VCSType(String urlMark) {
		this.urlMark = urlMark;
	}
	
	String getUrlMark() {
		return urlMark;
	}
}
