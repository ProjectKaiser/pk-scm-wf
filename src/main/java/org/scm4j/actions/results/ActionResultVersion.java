package org.scm4j.actions.results;

public class ActionResultVersion {
	private String name;
	private String version;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public ActionResultVersion() {
	}

	@Override
	public String toString() {
		return "ActionResultVersion [name=" + name + ", version=" + version + "]";
	}

}
