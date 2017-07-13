package org.scm4j.wf.conf;

public class Credentials {
	public static final Credentials EMPTY = new Credentials(null, null, false);
	
	private final String name;
	private final String password;
	private final Boolean isDefault;

	public Credentials(String name, String password, Boolean isDefault) {
		this.name = name;
		this.password = password;
		this.isDefault = isDefault;
	}

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

	public String getName() {
		return name;
	}

	public String getPassword() {
		return password;
	}

	public Boolean getIsDefault() {
		return isDefault;
	}

	@Override
	public String toString() {
		return "Credentials [name=" + name + "]";
	}
}
