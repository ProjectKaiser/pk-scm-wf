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

	private final boolean usePatch;
	private final boolean useSnapshot;
	
	public Version(String verStr) {
		this(verStr, true, true);
	}
	
	private Version(String verStr, boolean usePatch, boolean useSnapshot) {
		this.verStr = verStr;
		this.usePatch = usePatch;
		this.useSnapshot = useSnapshot;
		if (verStr.isEmpty()) {
			snapshot = "";
			prefix = "";
			minor = "";
			patch = "";
			isEmpty = true;
		} else {
			isEmpty = false;
			if (verStr.contains(SNAPSHOT)) {
				snapshot = SNAPSHOT;
				verStr = verStr.replace(SNAPSHOT, "");
			} else {
				snapshot = "";
			}
			if (verStr.lastIndexOf(".") > 0) {
				patch = verStr.substring(verStr.lastIndexOf("."), verStr.length());
				verStr = verStr.substring(0, verStr.lastIndexOf("."));
				if (verStr.lastIndexOf(".") > 0) {
					minor = verStr.substring(verStr.lastIndexOf(".") + 1, verStr.length());
				} else {
					minor = verStr;
				}
				prefix = verStr.substring(0, verStr.lastIndexOf(".") + 1);
			} else {
				prefix = "0.";
				minor = verStr;
				patch = ".0";
			}
			if (!minor.isEmpty() && !StringUtils.isNumeric(minor)) {
				throw new IllegalArgumentException("wrong version: " + verStr);
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
	
	public Version usePatch(boolean usePatch) {
		return new Version(verStr, usePatch, useSnapshot);
	}
	
	public Version useSnapshot(boolean useSnapshot) {
		return new Version(verStr, usePatch, useSnapshot);
	}
	
	public Version toNextPatch() {
		int i = 0;
		while (i < patch.length() && !Character.isDigit(patch.charAt(i))) i++;
		int firstDigitStart = i;
		while (i < patch.length() && Character.isDigit(patch.charAt(i))) i++;
		if (i == firstDigitStart) {
			return new Version(prefix + minor + patch + "1" + snapshot);
		}
		int patchInt = Integer.parseInt(patch.substring(firstDigitStart, i)) + 1;
		String newPatch = patch.substring(0, firstDigitStart) + Integer.toString(patchInt) +  patch.substring(i, patch.length());
		return clone(prefix + minor + newPatch + snapshot);
	}

	public Version toPreviousMinor() {
		checkMinor();
		return clone(prefix + Integer.toString(Integer.parseInt(minor) - 1) + patch + snapshot);
	}

	public Version toNextMinor() {
		checkMinor();
		return clone(prefix + Integer.toString(Integer.parseInt(minor) + 1) + patch + snapshot);
	}
	
	private Version clone(String verStr) {
		return new Version(verStr, usePatch, useSnapshot);
	}

	private void checkMinor() {
		if (!StringUtils.isNumeric(minor)) {
			throw new IllegalArgumentException("wrong version" + verStr);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Version other = (Version) obj;
		if (minor == null) {
			if (other.minor != null)
				return false;
		} else if (!minor.equals(other.minor))
			return false;
		if (patch == null) {
			if (other.patch != null)
				return false;
		} else if (!patch.equals(other.patch))
			return false;
		if (prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!prefix.equals(other.prefix))
			return false;
		if (usePatch != other.usePatch)
			return false;
		if (useSnapshot != other.useSnapshot)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((minor == null) ? 0 : minor.hashCode());
		result = prime * result + ((patch == null) ? 0 : patch.hashCode());
		result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
		result = prime * result + (usePatch ? 1231 : 1237);
		result = prime * result + (useSnapshot ? 1231 : 1237);
		return result;
	}

	public Boolean isEmpty() {
		return isEmpty;
	}
	
	public boolean isExactVersion() {
		return !minor.isEmpty();
	}

	public String toReleaseString() {
		return useSnapshot(false).toString();
	}
}
