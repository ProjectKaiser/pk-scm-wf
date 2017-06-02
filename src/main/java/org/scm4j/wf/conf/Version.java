package org.scm4j.wf.conf;

public class Version {

	private static final String SNAPSHOT = "-SNAPSHOT";

	private String minor;
	private String major;
	private String verPrefix;
	private Boolean isSnapshot;

	public Version(String ver) {

		String[] strs = ver.split("\\.");
		StringBuilder sb = new StringBuilder();
		String prefix = "";
		for (Integer i = 0; i <= strs.length - 3; i++) {
			sb.append(prefix);
			prefix = ".";
			sb.append(strs[i]);
		}
		verPrefix = sb.toString();

		// последние 2 числа будем смотерть
		major = strs[strs.length - 2];
		minor = strs[strs.length - 1];
		isSnapshot = minor.contains(SNAPSHOT);
		minor = minor.replace(SNAPSHOT, "");
	}

	public void setSnapshot(Boolean isSnapshot) {
		this.isSnapshot = isSnapshot;
	}

	public String getMinor() {
		return minor;
	}

	public void setMinor(String minor) {
		this.minor = minor;
	}

	@Override
	public String toString() {
		return verPrefix + "." + major + "." + minor + (isSnapshot ? SNAPSHOT : "");
	}

}
