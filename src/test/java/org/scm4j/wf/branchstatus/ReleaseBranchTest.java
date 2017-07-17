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
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.TestEnvironment;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;

@PrepareForTest(SCMWorkflow.class)
@RunWith(PowerMockRunner.class)
public class ReleaseBranchTest {
	
	private TestEnvironment env;
	private VCSRepositories repos;
	
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
		repos = SCMWorkflow.getReposFromEnvironment();
	}
	
	@After
	public void tearDown() throws IOException {
		env.clean();
	}
	
	@Test
	public void testMissing() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILLDB, repos);
		ReleaseBranch rb = new ReleaseBranch(comp, repos);
		ReleaseBranchStatus rbs = rb.getStatus();
		assertNotNull(rbs);
		assertEquals(rbs, ReleaseBranchStatus.MISSING);
	}
	
	
	@Test
	public void testBranched() throws Exception {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILLDB, repos);
		ReleaseBranch rb = new ReleaseBranch(comp, repos);
		env.getUnTillDbVCS().createBranch(null, rb.getReleaseBranchName(), null);
		
		ReleaseBranchStatus rbs = rb.getStatus();
		assertNotNull(rbs);
		assertEquals(rbs, ReleaseBranchStatus.BRANCHED);
	}
	
	@Test
	public void testBuilt() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILLDB, repos);
		ReleaseBranch rb = new ReleaseBranch(comp, repos);
		env.getUnTillDbVCS().createBranch(null, rb.getReleaseBranchName(), null);
		env.generateLogTag(env.getUnTillDbVCS(), rb.getReleaseBranchName(), LogTag.SCM_BUILT);
		
		ReleaseBranchStatus rbs = rb.getStatus();
		assertNotNull(rbs);
		assertEquals(rbs, ReleaseBranchStatus.BUILT);
	}
	
	@Test
	public void testTagged() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILLDB, repos);
		ReleaseBranch rb = new ReleaseBranch(comp, repos);
		env.getUnTillDbVCS().createBranch(null, rb.getReleaseBranchName(), null);
		env.generateLogTag(env.getUnTillDbVCS(), rb.getReleaseBranchName(), LogTag.SCM_VER);
		DevelopBranch db = new DevelopBranch(comp);
		String tagName = db.getVersion().toPreviousMinorRelease();
		env.getUnTillDbVCS().createTag(rb.getReleaseBranchName(), tagName, "release " + tagName);
		
		ReleaseBranchStatus rbs = rb.getStatus();
		assertNotNull(rbs);
		assertEquals(rbs, ReleaseBranchStatus.TAGGED);
	}
	
	@Test
	public void testMDepsTagged () {
		Component compUnTill = new Component(TestEnvironment.PRODUCT_UNTILL, repos);
		Component compUnTillDb = new Component(TestEnvironment.PRODUCT_UNTILLDB, repos); 
		Component compUBL= new Component(TestEnvironment.PRODUCT_UBL, repos);
		
		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill, repos);
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb, repos);
		ReleaseBranch rbUBL = new ReleaseBranch(compUBL, repos);
		
		env.getUnTillVCS().createBranch(null, rbUnTill.getReleaseBranchName(), null);
		env.getUnTillDbVCS().createBranch(null, rbUnTillDb.getReleaseBranchName(), null);
		env.getUblVCS().createBranch(null, rbUBL.getReleaseBranchName(), null);
		
		String unTillDbTagName = new DevelopBranch(compUnTillDb).getVersion().toReleaseString();
		env.getUnTillDbVCS().createTag(rbUnTillDb.getReleaseBranchName(), unTillDbTagName, "release " + unTillDbTagName);
		env.generateLogTag(env.getUnTillDbVCS(), rbUnTillDb.getReleaseBranchName(), LogTag.SCM_BUILT);
		
		ReleaseBranchStatus rbs = rbUnTill.getStatus();
		assertNotNull(rbs);
		assertEquals(ReleaseBranchStatus.BRANCHED, rbs);
		
		String ublTagName = new DevelopBranch(compUBL).getVersion().toReleaseString();
		env.getUblVCS().createTag(rbUBL.getReleaseBranchName(), ublTagName, "release " + ublTagName);
		env.generateLogTag(env.getUblVCS(), rbUBL.getReleaseBranchName(), LogTag.SCM_BUILT);
		
		rbs =  rbUnTill.getStatus();
		assertNotNull(rbs);
		assertEquals(ReleaseBranchStatus.MDEPS_TAGGED, rbs);
	}
	
	@Test
	public void testBranchStatusReleaseMDepsFrozen() {
		
	}

}
