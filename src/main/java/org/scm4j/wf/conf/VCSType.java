package org.scm4j.wf.conf;

public enum VCSType {
	GIT(".git"), SVN("");
	
	String strValue;
	
	VCSType(String strValue) {
		this.strValue = strValue;
	}
	
	public String getSuffix() {
		return strValue;
	}
}
