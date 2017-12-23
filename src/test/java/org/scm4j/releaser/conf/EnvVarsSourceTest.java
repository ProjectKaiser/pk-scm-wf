package org.scm4j.releaser.conf;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class EnvVarsSourceTest {

	private static final String CREDS_URL = "creds url";
	private static final String REPOS_URL = "repos url";
	
	@Rule
	public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
	
	@Test
	@Ignore
	public void testEnvVars() {
//		environmentVariables.set(EnvVarsConfigSource.CC_LOCATION_ENV_VAR, REPOS_URL);
//		environmentVariables.set(EnvVarsConfigSource.CREDENTIALS_LOCATION_ENV_VAR, CREDS_URL);
//		assertEquals(REPOS_URL, new EnvVarsConfigSource().getCompConfigLocations());
//		assertEquals(CREDS_URL, new EnvVarsConfigSource().getCredentialsLocations());
	}
}
