package org.scm4j.wf.conf;

import org.apache.commons.lang3.StringUtils;

public class Version {

	private static final String SNAPSHOT = "-SNAPSHOT";

	private String minor;
	private final String prefix;
	private final String snapshot;
	private final String patch;

	public Version(String ver) {
		if (ver.isEmpty()) {
			snapshot = "";
			prefix = "";
			minor = "";
			patch = "";
		} else {
			if (ver.contains(SNAPSHOT)) {
				snapshot = SNAPSHOT;
				ver = ver.replace(SNAPSHOT, "");
			} else {
				snapshot = "";
			}
			if (ver.lastIndexOf(".") > 0) {
				patch = ver.substring(ver.lastIndexOf("."), ver.length());
				ver = ver.substring(0, ver.lastIndexOf("."));
				if (ver.lastIndexOf(".") > 0) {
					minor = ver.substring(ver.lastIndexOf(".") + 1, ver.length());
				} else {
					minor = ver;
				}
				prefix = ver.substring(0, ver.lastIndexOf(".") + 1);
			} else {
				prefix ="0.";
				minor = ver;
				patch = ".0";
			}
		}
		if (!StringUtils.isNumeric(minor)) {
			throw new IllegalArgumentException("wrong version" + ver);
		}
	}

	public String getMinor() {
		return minor;
	}

	public void setMinor(String minor) {
		this.minor = minor;
	}
	
	public String getSnapshot() {
		return snapshot;
	}

	@Override
	public String toString() {
		return toReleaseString() + snapshot;
	}
	
	public String toReleaseString() {
		return prefix + minor + patch;
	}
}
