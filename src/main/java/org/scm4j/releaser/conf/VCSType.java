package org.scm4j.releaser.conf;

public enum VCSType {
	GIT(".git"), SVN("");
	
	final String strValue;
	
	VCSType(String strValue) {
		this.strValue = strValue;
	}
}
