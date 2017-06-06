package org.scm4j.wf.model;

import java.lang.reflect.Type;
import java.util.List;

import org.scm4j.wf.GsonUtils;

import com.google.gson.reflect.TypeToken;

public class Credentials {
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		Credentials other = (Credentials) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

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
	
	public static List<Credentials> fromJson(String jsonString) {
		Type type = new TypeToken<List<Credentials>>() {}.getType();
		return GsonUtils.fromJson(jsonString, type);
	}
}
