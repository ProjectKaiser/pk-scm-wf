package org.scm4j.wf;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.scm4j.actions.IAction;
import org.scm4j.wf.exceptions.EWFConfig;
import org.scm4j.wf.model.Credentials;
import org.scm4j.wf.model.VCSRepository;

@PrepareForTest({VCSRepository.class, Credentials.class})
@RunWith(PowerMockRunner.class)
public class SCMWorkflowConfigTest {

	private static final String TEST_ENVIRONMENT_DIR = TestEnvironment.TEST_ENVIRONMENT_DIR;
	private static final String TEST_VCS_REPO_FILE_URL = TestEnvironment.TEST_VCS_REPO_FILE_URL;
	
	private TestEnvironment env;

	@Before
	public void setUp() throws IOException {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		PowerMockito.mockStatic(System.class);
		PowerMockito.when(System.getenv(Credentials.CREDENTIALS_LOCATION_ENV_VAR))
				.thenReturn("file:///" + env.getCredsFile().getPath().replace("\\", "/"));
		PowerMockito.when(System.getenv(VCSRepository.REPOS_LOCATION_ENV_VAR))
				.thenReturn("file:///" + env.getReposFile().getPath().replace("\\", "/"));
	}

	@After
	public void tearDown() {
		File testFolder = new File(TEST_ENVIRONMENT_DIR);
		if (testFolder.exists()) {
			testFolder.delete();
		}
	}
	
	@Test
	public void testNoRelease() {
		SCMWorkflow wf = new SCMWorkflow("eu.untill:unTill");
		IAction action = wf.getProductionReleaseAction();
		assertNotNull(action);
		
		
		
	}
	
	

	
}
