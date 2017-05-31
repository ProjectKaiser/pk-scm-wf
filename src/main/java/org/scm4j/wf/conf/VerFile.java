package org.scm4j.wf.conf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerFile extends ConfFile {

	private static final int DEFAULT_VERSION = 0;
	private static final Pattern VERSION_PATTERN = Pattern.compile("[0-9]+\\g");
	private static final int MINOR_POSITION = 2;
	
	private String ver;
	private String release;
	private String branchType;
	private String releaseBranchPrefix;
	
	public int getMinor() {
		Matcher matcher = VERSION_PATTERN.matcher(ver);
		matcher.matches();
		if (matcher.groupCount() > 1) {
			return Integer.parseInt(matcher.group(MINOR_POSITION));
		}
		return DEFAULT_VERSION;
	}
	
	public void setMinor(int value) {
		Matcher matcher = VERSION_PATTERN.matcher(ver);
		if (matcher.groupCount() > 1) {
			ver = ver.substring(0, matcher.start(MINOR_POSITION)) + Integer.toString(value) +
					ver.substring(matcher.end(MINOR_POSITION), ver.length() - 1);
		} else if (matcher.groupCount() == 0) {
			ver = Integer.toString(DEFAULT_VERSION) + "." + Integer.toString(value) + ".0." + ver;
		} else {
			ver = ver.substring(0, matcher.end(1)) + "." + Integer.toString(value) + ".0." + ver.substring(
					matcher.end(1), ver.length() - 1);	
		}
	}

	public void setNumberGroupValueFromEnd(int groupNumber, int value) {
		Matcher matcher = VERSION_PATTERN.matcher(ver);
		if (matcher.find() && matcher.groupCount() >= groupNumber) {
			ver = ver.substring(0, matcher.start(matcher.groupCount() - groupNumber)) + Integer.toString(value) +
					ver.substring(matcher.end(matcher.groupCount() - groupNumber), ver.length() - 1);
		} else {
			ver = ver + Integer.toString(value);
		}
	}

	public String getVer() {
		return ver;
	}

	public String getReleaseBranchPrefix() {
		return releaseBranchPrefix;
	}

	public void setReleaseBranchPrefix(String releaseBranchPrefix) {
		this.releaseBranchPrefix = releaseBranchPrefix;
	}

	public void setVer(String ver) {
		this.ver = ver;
	}

	public String getRelease() {
		return release;
	}

	public void setRelease(String release) {
		this.release = release;
	}

	public String getBranchType() {
		return branchType;
	}

	public void setBranchType(String branchType) {
		this.branchType = branchType;
	}

	public VerFile(String content) {
		super(content);
	}
	
	@Override
	public String toString() {
		return "VerFile [ver=" + ver + "]";
	}

}
