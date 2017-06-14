package org.scm4j.wf;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.scm4j.wf.exceptions.EWFConfig;
import org.scm4j.wf.model.Credentials;
import org.scm4j.wf.model.VCSRepository;

@PrepareForTest({VCSRepository.class, Credentials.class})
@RunWith(PowerMockRunner.class)
public class ConfigTest {
	
	private static final String TEST_ENVIRONMENT_DIR = TestEnvironment.TEST_ENVIRONMENT_DIR;
	
	@Before
	public void setUp() {
		PowerMockito.mockStatic(System.class);
	}
	
	@Test
	public void testNoReposEnvVar() {
		PowerMockito.when(System.getenv(VCSRepository.REPOS_LOCATION_ENV_VAR))
				.thenReturn(null);
		try {
			new SCMWorkflow("eu.untill:unTill", TEST_ENVIRONMENT_DIR);
			fail();
		} catch (EWFConfig e) {
		}
	}
	
	@Test
	public void testMalformedReposUrl() {
		
		PowerMockito.when(System.getenv(VCSRepository.REPOS_LOCATION_ENV_VAR))
				.thenReturn("malformed url");
		try {
			new SCMWorkflow("eu.untill:unTill", TEST_ENVIRONMENT_DIR);
			fail();
		} catch (EWFConfig e) {
		}
	}
	
	@Test 
	public void testWrongReposLocation() {
		PowerMockito.when(System.getenv(VCSRepository.REPOS_LOCATION_ENV_VAR))
				.thenReturn("file:///c:/wrong/Location");
		try {
			new SCMWorkflow("eu.untill:unTill", TEST_ENVIRONMENT_DIR);
			fail();
		} catch (EWFConfig e) {
		}
	}
	
	@Test
	public void testBadReposFileContent() throws IOException {
		File vcsRepos = new File(TEST_ENVIRONMENT_DIR, "vcs-repos");
		vcsRepos.createNewFile();
		FileUtils.writeStringToFile(vcsRepos, "wrong content", StandardCharsets.UTF_8);
		PowerMockito.mockStatic(System.class);
		PowerMockito.when(System.getenv(VCSRepository.REPOS_LOCATION_ENV_VAR))
				.thenReturn("file:///" + vcsRepos.getParent().replace("\\", "/"));
		try { 
			new SCMWorkflow("eu.untill:unTill", TEST_ENVIRONMENT_DIR);
			fail();
		} catch (EWFConfig e) {
		}
	}
}
