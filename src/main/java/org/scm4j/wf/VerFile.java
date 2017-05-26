package org.scm4j.wf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerFile {
	
	private static final int DEFAULT_VERSION = 1;
	private static final Pattern LAST_NUMBER_PATTERN = Pattern.compile("[0-9]+$");
	
	private String ver;
	private String lastVerCommit;
	private String verCommit;
	private String releaseBranchPrefix;
	
	public int getLastNumber() {
		Matcher matcher = LAST_NUMBER_PATTERN.matcher(ver);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}
		return DEFAULT_VERSION;
	}
	
	public void setLastNumber(int number) {
		Matcher matcher = LAST_NUMBER_PATTERN.matcher(ver);
		if (matcher.find()) {
			matcher.replaceFirst(Integer.toString(number));
		} else {
			ver = ver + Integer.toString(number);
		}
	}

	public String getReleaseBranchPrefix() {
		return releaseBranchPrefix;
	}

	public void setReleaseBranchPrefix(String releaseBranchPrefix) {
		this.releaseBranchPrefix = releaseBranchPrefix;
	}

	public String getVer() {
		return ver;
	}

	public void setVer(String ver) {
		this.ver = ver;
	}

	public String getLastVerCommit() {
		return lastVerCommit;
	}

	public void setLastVerCommit(String lastVerCommit) {
		this.lastVerCommit = lastVerCommit;
	}

	public String getVerCommit() {
		return verCommit;
	}

	public void setVerCommit(String verCommit) {
		this.verCommit = verCommit;
	}

	public VerFile() {
	}

	@Override
	public String toString() {
		return "VerFile [ver=" + ver + ", lastVerCommit=" + lastVerCommit + ", verCommit=" + verCommit + "]";
	}
}
