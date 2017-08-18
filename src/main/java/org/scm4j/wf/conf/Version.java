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

	private boolean usePatch = true;
	private boolean useSnapshot = true;
	
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
	
	public String getPatch() {
		return usePatch ? patch : "";
	}

	public String getMinor() {
		return minor;
	}

	public String getSnapshot() {
		return useSnapshot ? snapshot : "";
	}

	@Override
	public String toString() {
		if (!StringUtils.isNumeric(minor)) {
			return verStr;
		}
		return prefix + minor + getPatch() + getSnapshot();
	}
	
	public Version usePatch(boolean usePath) {
		this.usePatch = usePath;
		return this;
	}
	
	public Version useSnapshot(boolean useSnapshot) {
		this.useSnapshot = useSnapshot;
		return this;
	}
	
	public Version toNextPatch() {
		if (patch.isEmpty()) {
			return new Version(prefix + minor + ".1" + snapshot);
		}
		int i = 0;
		while (i < patch.length() && Character.isDigit(patch.charAt(i))) i++;
		if (i == 0) {
			return new Version(prefix + minor + patch + ".1" + snapshot);
		}
		int patchInt = Integer.parseInt(patch.substring(0, i)) + 1;
		String newPatch = Integer.toString(patchInt) + patch.substring(i, patch.length());
		return new Version(prefix + minor + newPatch + snapshot);
	}

	public Version toPreviousMinor() {
		checkMinor();
		return new Version(prefix + Integer.toString(Integer.parseInt(minor) - 1) + patch + snapshot);
	}

	public Version toNextMinor() {
		checkMinor();
		return new Version(prefix + Integer.toString(Integer.parseInt(minor) + 1) + patch + snapshot);
	}
	
	private void checkMinor() {
		if (!StringUtils.isNumeric(minor)) {
			throw new IllegalArgumentException("wrong version" + verStr);
		}
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

	public String toReleaseString() {
		return new Version(verStr).useSnapshot(false).toString();
	}
}
