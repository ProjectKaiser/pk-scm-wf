package org.scm4j.wf;

public class Credentials {
	private String name;
	private String password;
	private Boolean isDefault = false;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Boolean getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	public Credentials() {

	}

	@Override
	public String toString() {
		return "Credentials [name=" + name + "]";
	}
}
