package org.scm4j.releaser.conf;

import org.apache.commons.io.FileUtils;
import org.scm4j.commons.CommentedString;
import org.scm4j.commons.URLContentLoader;
import org.scm4j.releaser.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DefaultConfigUrls implements IConfigUrls {
	
	public static final File PRIORITY_CC_FILE = new File(Utils.BASE_WORKING_DIR, "cc.yml");
	public static final File CC_URLS_FILE = new File(Utils.BASE_WORKING_DIR, "cc");
	public static final File CREDENTIALS_FILE = new File(Utils.BASE_WORKING_DIR, "credentials.yml");
	public static final String CC_URLS_ENV_VAR = "SCM4J_CC";
	public static final String CREDENTIALS_URL_ENV_VAR = "SCM4J_CREDENTIALS";
	public static final String URL_SEPARATOR = URLContentLoader.URL_SEPARATOR;

	@Deprecated
	public static final String REPOS_LOCATION_ENV_VAR = "SCM4J_VCS_REPOS";
	
	@Override
	public String getCCUrls() {
		try {
			String separatedUrls = getCCUrlsFromEnvVar();
			if (separatedUrls != null) {
				return separatedUrls;
			}
			
			StringBuilder urlsSB = new StringBuilder();
			
			if (PRIORITY_CC_FILE.exists()) {
				urlsSB.append(PRIORITY_CC_FILE.toString() + URL_SEPARATOR);
			}
			
			if (CC_URLS_FILE.exists()) {
				List<String> lines = getLinesFromCCFile();
				for (String line : lines) {
					CommentedString cs = new CommentedString(line);
					if (cs.isValuable()) {
						urlsSB.append(cs.getStrNoComment().trim() + URL_SEPARATOR);
					}
				}
			}
			
			if (urlsSB.length() > 0) {
				urlsSB.setLength(urlsSB.length() - URL_SEPARATOR.length());
				return urlsSB.toString();
			}

			return null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	List<String> getLinesFromCCFile() throws IOException {
		return FileUtils.readLines(CC_URLS_FILE, StandardCharsets.UTF_8);
	}
	
	@Override
	public String getCredsUrl() {
		String credentialsUrl = getEnv(CREDENTIALS_URL_ENV_VAR);
		if (credentialsUrl != null) {
			return credentialsUrl;
		}
		if (CREDENTIALS_FILE.exists()) {
			return CREDENTIALS_FILE.toString();
		}
		return null;
	}

	String getEnv(String name) {
		return System.getenv(name);
	}

	private String getCCUrlsFromEnvVar() {
		String res = getEnv(REPOS_LOCATION_ENV_VAR);
		if (res == null) {
			res = getEnv(CC_URLS_ENV_VAR);
		}
		return res;
	}
}
