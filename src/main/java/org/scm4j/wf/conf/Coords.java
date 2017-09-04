package org.scm4j.wf.conf;

import org.apache.commons.lang3.StringUtils;

public class Coords {

	private final String artifactId;
	private final String commentStr;
	private final String extension;
	private final String groupId;
	private final String classifier;
	private final Version version;
	private final String coordsString;

	public String getComment() {
		return commentStr;
	}
	
	public Coords(String coordsString) {
		this.coordsString = coordsString;
		String str = coordsString;

		// Comment
		{
			Integer pos = coordsString.indexOf("#");
			
			if (pos > 0) {
				// add spaces between valuable chars and # to comment
				commentStr = StringUtils.difference(str.substring(0, pos).trim(), str.substring(0, pos)) + str.substring(pos); 
				str = str.substring(0, pos).trim();
			} else {
				commentStr = "";
			}
		}

		// Extension
		{
			Integer pos = coordsString.indexOf("@");
			if (pos > 0) {
				extension = str.substring(pos).trim();
				str = str.substring(0, pos);
			} else {
				extension = "";
			}
		}

		String[] strs = str.split(":", -1);
		if (strs.length < 2) {
			throw new IllegalArgumentException("wrong mdep coord: " + coordsString);
		}

		groupId = strs[0];
		artifactId = strs[1];

		classifier = strs.length > 3 ? ":" + strs[3].trim() : "";

		version = new Version(strs.length > 2 ? strs[2] : "");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((coordsString == null) ? 0 : coordsString.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Coords other = (Coords) obj;
		if (coordsString == null) {
			if (other.coordsString != null)
				return false;
		} else if (!coordsString.equals(other.coordsString))
			return false;
		return true;
	}

	public Version getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return toString(version.toString());
	}

	public String toString(String versionStr) {
		String str = versionStr + classifier + extension;
		return getName() + (str.isEmpty() ? "" : ":" + str) + commentStr;
	}
	
	public String getName() {
		return groupId + ":" + artifactId;
	}
	
	public String getGroupId() {
		return groupId;
	}
	
	public String getArtifactId() {
		return artifactId;
	}

	public String getExtension() {
		return extension;
	}
	
	public String getClassifier() {
		return classifier;
	}
}
