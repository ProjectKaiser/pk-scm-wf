package org.scm4j.wf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.scm4j.wf.exceptions.EConfig;

@PrepareForTest(SCMWorkflow.class)
@RunWith(PowerMockRunner.class)
public class ConfigTest {
	
	private static final String TEST_ENVIRONMENT_DIR = TestEnvironment.TEST_ENVIRONMENT_DIR;
	
	@Before
	public void setUp() {
		PowerMockito.mockStatic(System.class);
	}
	
	@Test
	public void testNoReposEnvVar() {
		PowerMockito.when(System.getenv(SCMWorkflow.REPOS_LOCATION_ENV_VAR))
				.thenReturn(null);
		try {
			new SCMWorkflow("eu.untill:unTill");
			fail();
		} catch (EConfig e) {
			assertNull(e.getCause());
		}
	}
	
	@Test
	public void testMalformedReposUrl() {
		
		PowerMockito.when(System.getenv(SCMWorkflow.REPOS_LOCATION_ENV_VAR))
				.thenReturn("malformed url");
		try {
			new SCMWorkflow("eu.untill:unTill");
			fail();
		} catch (EConfig e) {
			assertTrue(e.getCause() instanceof MalformedURLException);
		}
	}
	
	@Test 
	public void testWrongReposLocation() {
		PowerMockito.when(System.getenv(SCMWorkflow.REPOS_LOCATION_ENV_VAR))
				.thenReturn("file:///c:/wrong/Location");
		try {
			new SCMWorkflow("eu.untill:unTill");
			fail();
		} catch (EConfig e) {
			assertTrue(e.getCause() instanceof IOException);
		}
	}
	
	@Test
	public void testBadReposFileContent() throws IOException {
		File vcsRepos = new File(TEST_ENVIRONMENT_DIR, "vcs-repos");
		vcsRepos.getParentFile().mkdirs();
		vcsRepos.createNewFile();
		FileUtils.writeStringToFile(vcsRepos, "wrong content", StandardCharsets.UTF_8);
		PowerMockito.mockStatic(System.class);
		PowerMockito.when(System.getenv(SCMWorkflow.REPOS_LOCATION_ENV_VAR))
				.thenReturn("file:///" + vcsRepos.getAbsolutePath().replace("\\", "/"));
		PowerMockito.when(System.getenv(SCMWorkflow.CREDENTIALS_LOCATION_ENV_VAR))
				.thenReturn("file:///" + vcsRepos.getAbsolutePath().replace("\\", "/"));
		try { 
			new SCMWorkflow("eu.untill:unTill");
			fail();
		} catch (EConfig e) {
			assertNotNull(e.getCause());
		}
	}
}
