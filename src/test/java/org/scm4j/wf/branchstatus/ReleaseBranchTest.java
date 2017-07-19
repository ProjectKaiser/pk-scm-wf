package org.scm4j.wf.branchstatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.TestEnvironment;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.VCSRepositories;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ReleaseBranchTest {
	
	private TestEnvironment env;
	private VCSRepositories repos;
	
	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		repos = VCSRepositories.loadVCSRepositories();
	}
	
	@After
	public void tearDown() throws Exception {
		env.close();
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
		
		String unTillDbTagName = new DevelopBranch(compUnTillDb).getVersion().toPreviousMinorRelease();
		env.generateLogTag(env.getUnTillDbVCS(), rbUnTillDb.getReleaseBranchName(), LogTag.SCM_BUILT);
		env.getUnTillDbVCS().createTag(rbUnTillDb.getReleaseBranchName(), unTillDbTagName, "release " + unTillDbTagName);
		
		ReleaseBranchStatus rbs = rbUnTill.getStatus();
		assertEquals(ReleaseBranchStatus.BRANCHED, rbs);
		
		String ublTagName = new DevelopBranch(compUBL).getVersion().toPreviousMinorRelease();
		env.generateLogTag(env.getUblVCS(), rbUBL.getReleaseBranchName(), LogTag.SCM_BUILT);
		env.getUblVCS().createTag(rbUBL.getReleaseBranchName(), ublTagName, "release " + ublTagName);
		
		rbs =  rbUnTill.getStatus();
		assertEquals(ReleaseBranchStatus.MDEPS_TAGGED, rbs);
	}
	
	@Test
	public void testMDepsFrozen() {
		Component compUnTill = new Component(TestEnvironment.PRODUCT_UNTILL, repos);
		Component compUnTillDb = new Component(TestEnvironment.PRODUCT_UNTILLDB, repos); 
		Component compUBL= new Component(TestEnvironment.PRODUCT_UBL, repos);
		
		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill, repos);
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb, repos);
		ReleaseBranch rbUBL = new ReleaseBranch(compUBL, repos);
		
		env.getUnTillVCS().createBranch(null, rbUnTill.getReleaseBranchName(), null);
		env.getUnTillDbVCS().createBranch(null, rbUnTillDb.getReleaseBranchName(), null);
		env.getUblVCS().createBranch(null, rbUBL.getReleaseBranchName(), null);
		
		// ubl: freeze unTillDb mdep 
		Component compUntillDbVersioned = new Component(TestEnvironment.PRODUCT_UNTILLDB + ":" + env.getUnTillDbVer().toString(), repos);
		MDepsFile ublMDepsFile = new MDepsFile(Arrays.asList(compUntillDbVersioned));
		env.getUblVCS().setFileContent(rbUBL.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, ublMDepsFile.toFileContent(), "mdeps versions frozen");
		
		ReleaseBranchStatus rbs = rbUnTill.getStatus();
		assertEquals(ReleaseBranchStatus.BRANCHED, rbs);
		
		// unTill: freeze UBL mdep
		Component compUBLVersioned = new Component(TestEnvironment.PRODUCT_UBL+ ":" + env.getUblVer().toString(), repos);
		MDepsFile unTillMDepsFile = new MDepsFile(Arrays.asList(compUBLVersioned, compUnTillDb));
		env.getUnTillVCS().setFileContent(rbUnTill.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, unTillMDepsFile.toFileContent(), "UBL mdep version frozen");
		
		rbs = rbUnTill.getStatus();
		assertEquals(ReleaseBranchStatus.BRANCHED, rbs); // unTill: unTillDb still not frozen
		
		// unTill: freeze unTillDb mdep
		unTillMDepsFile = new MDepsFile(Arrays.asList(compUBLVersioned, compUntillDbVersioned));
		env.getUnTillVCS().setFileContent(rbUnTill.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, unTillMDepsFile.toFileContent(), "unTillDb mdep version frozen");
		
		rbs = rbUnTill.getStatus();
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbs); // unTill: unTillDb still not frozen
	}
}
