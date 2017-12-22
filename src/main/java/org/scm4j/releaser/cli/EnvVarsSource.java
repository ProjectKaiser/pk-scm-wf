package org.scm4j.releaser.cli;

public class EnvVarsSource implements IEnvVarsSource {
	
	@Deprecated
	public static final String REPOS_LOCATION_ENV_VAR = "SCM4J_VCS_REPOS";

	public static final String CC_URLS_ENV_VAR = "SCM4J_CC";
	public static final String CREDENTIALS_URL_ENV_VAR = "SCM4J_CREDENTIALS";

	@Override
	public String getCCUrls() {
		String res = System.getenv(REPOS_LOCATION_ENV_VAR);
		if (res == null) {
			res = System.getenv(CC_URLS_ENV_VAR);
		}
		return res;
	}

	@Override
	public String getCredsUrl() {
		return System.getenv(CREDENTIALS_URL_ENV_VAR);
	}

}
