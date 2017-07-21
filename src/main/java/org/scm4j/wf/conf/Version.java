package org.scm4j.wf.conf;

import org.apache.commons.lang3.StringUtils;

public class Version {

	private static final String SNAPSHOT = "-SNAPSHOT";

	private final String minor;
	private final String prefix;
	private final String snapshot;
	private final String patch;
	private final String verStr;
	private final Boolean isEmpty;

	public Version(String ver) {
		verStr = ver;
		if (ver.isEmpty()) {
			snapshot = "";
			prefix = "";
			minor = "";
			patch = "";
			isEmpty = true;
		} else {
			isEmpty = false;
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
				prefix = "0.";
				minor = ver;
				patch = ".0";
			}
			if (!minor.isEmpty() && !StringUtils.isNumeric(minor)) {
				throw new IllegalArgumentException("wrong version: " + ver);
			}
		}
	}

	public String getMinor() {
		return minor;
	}

	public String getSnapshot() {
		return snapshot;
	}

	@Override
	public String toString() {
		if (!StringUtils.isNumeric(minor)) {
			return verStr;
		}
		return toReleaseString() + snapshot;
	}

	public String toReleaseString() {
		checkMinor();
		return prefix + minor + patch;
	}

	public String toPreviousMinorRelease() {
		checkMinor();
		return prefix + Integer.toString(Integer.parseInt(minor) - 1) + patch;
	}

	public String toNextMinorRelease() {
		checkMinor();
		return prefix + Integer.toString(Integer.parseInt(minor) + 1) + patch;
	}

	private void checkMinor() {
		if (!StringUtils.isNumeric(minor)) {
			throw new IllegalArgumentException("wrong version" + verStr);
		}
	}

	public String toNextMinorSnapshot() {
		checkMinor();
		return prefix + Integer.toString(Integer.parseInt(minor) + 1) + patch + snapshot;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Version version = (Version) o;
		return !(verStr != null ? !verStr.equals(version.verStr) : version.verStr != null);
	}

	@Override
	public int hashCode() {
		return verStr != null ? verStr.hashCode() : 0;
	}

	public Boolean isEmpty() {
		return isEmpty;
	}
	
	public boolean isExactVersion() {
		return !minor.isEmpty();
	}
}
