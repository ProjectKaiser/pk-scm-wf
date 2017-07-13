package org.scm4j.wf.branchstatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.TestEnvironment;
import org.scm4j.wf.conf.Dep;
import org.scm4j.wf.conf.VCSRepositories;

@PrepareForTest(SCMWorkflow.class)
@RunWith(PowerMockRunner.class)
public class BranchStatusTest {
	
	private TestEnvironment env;
	
	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		PowerMockito.mockStatic(System.class);
		PowerMockito.when(System.getenv(SCMWorkflow.CREDENTIALS_LOCATION_ENV_VAR))
				.thenReturn("file://localhost/" + env.getCredsFile().getPath().replace("\\", "/"));
		PowerMockito.when(System.getenv(SCMWorkflow.REPOS_LOCATION_ENV_VAR))
				.thenReturn("file://localhost/" + env.getReposFile().getPath().replace("\\", "/"));
		PowerMockito.when(System.getProperty(Matchers.anyString()))
				.thenCallRealMethod();
	}
	
	@After
	public void tearDown() throws IOException {
		env.clean();
	}
	
	
	@Test
	public void testBranchStatusNothingIsMade() {
		BranchStatus bs = new BranchStatus();
		VCSRepositories repos = SCMWorkflow.getReposFromEnvironment();
		Dep dep = new Dep("eu.untill:unTill", repos);
		
		BranchStatuses bss = bs.getCurrentStatuses(dep, repos);
		assertNotNull(bss);
		assertEquals(bss.getDevelopStatus(), DevelopBranchStatus.MODIFIED);
		assertEquals(bss.getReleaseStatus(), ReleaseBranchStatus.MISSING);
	}
	
	@Test
	public void testBranchStatusHasFeatureCommits() {
		env.generateFeatureCommit(env.getUnTillVCS(), "feature commit");
		
	}

}
