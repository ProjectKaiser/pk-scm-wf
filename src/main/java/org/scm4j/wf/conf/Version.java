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

	public Version(String verStr) {
		this.verStr = verStr;
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
				patch = verStr.substring(verStr.lastIndexOf(".") + 1, verStr.length());
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
				patch = "0";
			}
			if (!minor.isEmpty() && !StringUtils.isNumeric(minor)) {
				throw new IllegalArgumentException("wrong version: " + verStr);
			}
		}
	}

	public String getPatch() {
		return patch;
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
		return prefix + minor + (patch.isEmpty() ? "" : "." + patch) + snapshot;
	}

	public String toPreviousPatch() {
		int i = 0;
		while (i < patch.length() && !Character.isDigit(patch.charAt(i)))
			i++;
		int firstDigitStart = i;
		while (i < patch.length() && Character.isDigit(patch.charAt(i)))
			i++;
		if (i == firstDigitStart) {
			return prefix + minor + "." + patch + "0" + snapshot;
		}
		int patchInt = Integer.parseInt(patch.substring(firstDigitStart, i)) - 1;
		String newPatch = patch.substring(0, firstDigitStart) + Integer.toString(patchInt)
				+ patch.substring(i, patch.length());
		return prefix + minor + "." + newPatch + snapshot;
	}

	public Version toNextPatch() {
		int i = 0;
		while (i < patch.length() && !Character.isDigit(patch.charAt(i)))
			i++;
		int firstDigitStart = i;
		while (i < patch.length() && Character.isDigit(patch.charAt(i)))
			i++;
		if (i == firstDigitStart) {
			return new Version(prefix + minor + "." + patch + "1" + snapshot);
		}
		int patchInt = Integer.parseInt(patch.substring(firstDigitStart, i)) + 1;
		String newPatch = patch.substring(0, firstDigitStart) + Integer.toString(patchInt)
				+ patch.substring(i, patch.length());
		return new Version(prefix + minor + "." + newPatch + snapshot);
	}

	public Version toPreviousMinor() {
		checkMinor();
		return new Version(prefix + Integer.toString(Integer.parseInt(minor) - 1) + "." + patch + snapshot);
	}

	public Version toNextMinor() {
		checkMinor();
		return new Version(prefix + Integer.toString(Integer.parseInt(minor) + 1) + "." + patch + snapshot);
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
		if (verStr == null) {
			if (other.verStr != null)
				return false;
		} else if (!verStr.equals(other.verStr))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((verStr == null) ? 0 : verStr.hashCode());
		return result;
	}

	public Boolean isEmpty() {
		return isEmpty;
	}

	public boolean isExactVersion() {
		return !minor.isEmpty();
	}

	public String toReleaseString() {
		if (!StringUtils.isNumeric(minor)) {
			return verStr;
		}
		return prefix + minor + (patch.isEmpty() ? "" : "." + patch);
	}

	public Boolean isGreaterThan(Version other) {
		if (other.isEmpty() || !other.isExactVersion()) {
			return !isEmpty() && isExactVersion();
		}
		if (!StringUtils.isNumeric(getMinor()) || !StringUtils.isNumeric(other.getMinor())) {
			return false;
		}
		int minor = Integer.parseInt(getMinor());
		int otherMinor = Integer.parseInt(other.getMinor());
		if (minor > otherMinor) {
			return true;
		}
		if (minor < otherMinor) {
			return false;
		}

		if (!StringUtils.isNumeric(getPatch()) || !StringUtils.isNumeric(other.getPatch())) {
			return false;
		}

		int patch = Integer.parseInt(getPatch());
		int otherPatch = Integer.parseInt(other.getPatch());

		if (patch == otherPatch) {
			return false;
		}

		return patch > otherPatch;

	}

	public String getReleaseNoPatchString() {
		return prefix + minor;
	}

	public Version toRelease() {
		return new Version(prefix + minor + (patch.isEmpty() ? "" : "." + patch));
	}

}