package org.scm4j.wf.actions.results;

public class ActionResultVersion {
	private final String name;
	private final String version;
	private final Boolean isNewBuild;
	private final String newBranchName;
	
	public String getNewBranchName() {
		return newBranchName;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}
	
	public Boolean getIsNewBuild() {
		return isNewBuild;
	}

	public ActionResultVersion(String name, String version, Boolean isNewBuild, String newBranchName) {
		this.name = name;
		this.version = version;
		this.isNewBuild = isNewBuild;
		this.newBranchName = newBranchName;
	}

	@Override
	public String toString() {
		return "ActionResultVersion [name=" + name + ", version=" + version + ", isNewBuild=" + isNewBuild
				+ ", newBranchName=" + newBranchName + "]";
	}

}
