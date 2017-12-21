package org.scm4j.releaser.conf;

public class EnvVarsConfigSource implements IConfigSource {

	/**
	 *  @deprecated use {@link #CC_LOCATION_ENV_VAR} instead
	 */
	@Deprecated
	public static final String REPOS_LOCATION_ENV_VAR = "SCM4J_VCS_REPOS";
	public static final String CC_LOCATION_ENV_VAR = "SCM4J_CC";
	public static final String CREDENTIALS_LOCATION_ENV_VAR = "SCM4J_CREDENTIALS";

	@Override
	public String getCompConfigLocations() {
		String res = System.getenv(CC_LOCATION_ENV_VAR);
		return res != null ? res : System.getenv(REPOS_LOCATION_ENV_VAR);
	}

	@Override
	public String getCredentialsLocations() {
		return System.getenv(CREDENTIALS_LOCATION_ENV_VAR);
	}
}
