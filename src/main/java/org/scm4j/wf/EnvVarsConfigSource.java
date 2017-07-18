package org.scm4j.wf;

public class EnvVarsConfigSource implements IConfigSource {

	public static final String REPOS_LOCATION_ENV_VAR = "SCM4J_VCS_REPOS";
	public static final String CREDENTIALS_LOCATION_ENV_VAR = "SCM4J_CREDENTIALS";

	@Override
	public String getReposLocations() {
		return System.getenv(REPOS_LOCATION_ENV_VAR);
	}

	@Override
	public String getCredentialsLocations() {
		return System.getenv(CREDENTIALS_LOCATION_ENV_VAR);
	}
}
