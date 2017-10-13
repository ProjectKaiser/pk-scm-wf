package org.scm4j.releaser.conf;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class EnvVarsConfigSourceTest {

	private static final String CREDS_URL = "creds url";
	private static final String REPOS_URL = "repos url";
	
	@Rule
	public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
	
	@Test
	public void testEnvVars() {
		environmentVariables.set(EnvVarsConfigSource.REPOS_LOCATION_ENV_VAR, REPOS_URL);
		environmentVariables.set(EnvVarsConfigSource.CREDENTIALS_LOCATION_ENV_VAR, CREDS_URL);
		assertEquals(REPOS_URL, new EnvVarsConfigSource().getReposLocations());
		assertEquals(CREDS_URL, new EnvVarsConfigSource().getCredentialsLocations());
	}
}
