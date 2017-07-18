package org.scm4j.wf.branchstatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.wf.*;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ReleaseBranchTest {
	
	private TestEnvironment env;
	private VCSRepositories repos;
	
	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		SCMWorkflow.setConfigSource(new IConfigSource() {
			@Override
			public String getReposLocations() {
				return "file://localhost/" + env.getReposFile().getPath().replace("\\", "/");
			}

			@Override
			public String getCredentialsLocations() {
				return "file://localhost/" + env.getCredsFile().getPath().replace("\\", "/");
			}
		});
		repos = SCMWorkflow.loadVCSRepositories();
	}
	
	@After
	public void tearDown() throws IOException {
		env.clean();
		SCMWorkflow.setConfigSource(new EnvVarsConfigSource());
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
		assertEquals(ReleaseBranchStatus.BRANCHED, rbs);
		
		String ublTagName = new DevelopBranch(compUBL).getVersion().toReleaseString();
		env.getUblVCS().createTag(rbUBL.getReleaseBranchName(), ublTagName, "release " + ublTagName);
		env.generateLogTag(env.getUblVCS(), rbUBL.getReleaseBranchName(), LogTag.SCM_BUILT);
		
		rbs =  rbUnTill.getStatus();
		assertEquals(ReleaseBranchStatus.MDEPS_TAGGED, rbs);
	}
	
	@Test
	public void testBranchStatusReleaseMDepsFrozen() {
		
	}

}
