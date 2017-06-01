package org.scm4j.actions.results;

public class ActionResultVersion {
	private String name;
	private String version;
	private Boolean isNewBuild;

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}
	
	public Boolean getIsNewBuild() {
		return isNewBuild;
	}

	public ActionResultVersion(String name, String version, Boolean isNewBuild) {
		this.name = name;
		this.version = version;
		this.isNewBuild = isNewBuild;
	}

	@Override
	public String toString() {
		return "ActionResultVersion [name=" + name + ", version=" + version + ", isNewBuild=" + isNewBuild + "]";
	}

}
