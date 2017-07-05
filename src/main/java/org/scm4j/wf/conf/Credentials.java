package org.scm4j.wf.conf;

import com.google.common.base.Strings;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import org.scm4j.wf.GsonUtils;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Credentials {
	public static final String CREDENTIALS_LOCATION_ENV_VAR = "SCM4J_CREDENTIALS";
	
	private String name;
	private String password;
	private Boolean isDefault = false;

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
		if (Strings.isNullOrEmpty(jsonString)) {
			return new ArrayList<>();
		}
		Type type = new TypeToken<List<Credentials>>() {}.getType();
		return GsonUtils.fromJson(jsonString, type);
	}
	
	public static Map<String, Credentials> loadFromEnvironment() throws Exception {
		String storeUrlsStr = System.getenv(CREDENTIALS_LOCATION_ENV_VAR);
		Map<String, Credentials> res = new HashMap<>();
		if (storeUrlsStr == null) {
			return res;
		}
		String[] storeUrls = storeUrlsStr.split(";");
		for (String storeUrl : storeUrls) {
			URL url = new URL(storeUrl);
			String credsJson;
			try (InputStream inputStream = url.openStream()) {
				credsJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			for (Credentials cred : Credentials.fromJson(credsJson)) {
				res.put(cred.getName(), cred);
			}
		}
		return res;
	}
}
