package org.scm4j.wf.conf;

public class DepCoords {

	private final String nameStr;
	private final String commentStr;
	private final String extStr;
	private final String groupStr;
	private final String classStr;
	private final Version version;

	public String getComment() {
		return commentStr;
	}

	public DepCoords(String sourceString) {
		String str = sourceString;

		// Comment
		{
			Integer pos = sourceString.indexOf("#");
			
			if (pos > 0) {
				commentStr = str.substring(pos);
				str = str.substring(0, pos);
			} else {
				commentStr = "";
			}
		}

		// Extension
		{
			Integer pos = sourceString.indexOf("@");
			if (pos > 0) {
				extStr = str.substring(pos);
				str = str.substring(0, pos);
			} else {
				extStr = "";
			}
		}

		String[] strs = str.split(":", -1);
		if (strs.length < 2) {
			throw new IllegalArgumentException("wrong mdep coord: " + sourceString);
		}

		groupStr = strs[0];
		nameStr = strs[1];

		classStr = strs.length > 3 ? ":" + strs[3] : "";

		version = new Version(strs.length > 2 ? strs[2] : "");
	}

	public Version getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return toString(version.toString());
	}

	public String toString(String versionStr) {
		return getName() + ":" + versionStr + classStr + extStr + commentStr;
	}

	public String getName() {
		return groupStr + ":" + nameStr;
	}

	public String getExtension() {
		return extStr;
	}
	
	public String getClassifier() {
		return classStr;
	}

}
