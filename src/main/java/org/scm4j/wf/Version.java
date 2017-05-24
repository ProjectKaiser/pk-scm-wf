package org.scm4j.wf;

public class Version {
	private int major;
	private String suffix;
	private String verCommit;
	private String lastVerCommit;

	public int getMajor() {
		return major;
	}

	public void setMajor(int major) {
		this.major = major;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getVerCommit() {
		return verCommit;
	}

	public void setVerCommit(String verCommit) {
		this.verCommit = verCommit;
	}

	public String getLastVerCommit() {
		return lastVerCommit;
	}

	public void setLastVerCommit(String lastVerCommit) {
		this.lastVerCommit = lastVerCommit;
	}

	public Version() {
	}

}
