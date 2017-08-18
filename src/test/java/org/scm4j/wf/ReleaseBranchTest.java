package org.scm4j.wf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.Test;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.branch.DevelopBranch;
import org.scm4j.wf.branch.ReleaseBranchStatus;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.MDepsFile;

public class ReleaseBranchTest extends SCMWorkflowTestBase {
	
	@Test
	public void testMissing() {
		ReleaseBranchStatus rbs = rbUnTillDbFixedVer.getStatus();
		assertNotNull(rbs);
		assertEquals(rbs, ReleaseBranchStatus.MISSING);
	}
	
	@Test
	public void testBranchedIfNoMDeps() throws Exception {
		env.getUnTillDbVCS().createBranch(null, rbUnTillDbFixedVer.getReleaseBranchName(), null);
		
		ReleaseBranchStatus rbs = rbUnTillDbFixedVer.getStatus();
		assertNotNull(rbs);
		assertEquals(rbs, ReleaseBranchStatus.BRANCHED);
	}
	
	@Test
	public void testBranched() throws Exception {
		env.getUblVCS().createBranch(null, rbUBLFixedVer.getReleaseBranchName(), null);
		// ubl: freeze unTillDb mdep 
		MDepsFile ublMDepsFile = new MDepsFile(Arrays.asList(compUnTillDb));
		env.getUblVCS().setFileContent(rbUBLFixedVer.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, ublMDepsFile.toFileContent(), "mdeps versions frozen");
		
		ReleaseBranchStatus rbs = rbUBLFixedVer.getStatus();
		assertNotNull(rbs);
		assertEquals(rbs, ReleaseBranchStatus.BRANCHED);
	}
	
	@Test
	public void testBuilt() {
		env.getUnTillDbVCS().createBranch(null, rbUnTillDbFixedVer.getReleaseBranchName(), null);
		env.generateLogTag(env.getUnTillDbVCS(), rbUnTillDbFixedVer.getReleaseBranchName(), LogTag.SCM_BUILT);
		
		ReleaseBranchStatus rbs = rbUnTillDbFixedVer.getStatus();
		assertNotNull(rbs);
		assertEquals(rbs, ReleaseBranchStatus.BUILT);
	}
	
	@Test
	public void testTagged() {
		env.getUnTillDbVCS().createBranch(null, rbUnTillDbFixedVer.getReleaseBranchName(), null);
		env.generateLogTag(env.getUnTillDbVCS(), rbUnTillDbFixedVer.getReleaseBranchName(), LogTag.SCM_VER);
		String tagName = dbUnTillDb.getVersion().toPreviousMinorRelease();
		env.getUnTillDbVCS().createTag(rbUnTillDbFixedVer.getReleaseBranchName(), tagName, "release " + tagName);
		
		ReleaseBranchStatus rbs = rbUnTillDbFixedVer.getStatus();
		assertNotNull(rbs);
		assertEquals(rbs, ReleaseBranchStatus.TAGGED);
	}
	
	@Test
	public void testMDepsTagged () {
//		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill, repos);
//		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb, repos);
//		ReleaseBranch rbUBL = new ReleaseBranch(compUBL, repos);
		
		env.getUnTillVCS().createBranch(null, rbUnTillFixedVer.getReleaseBranchName(), null);
		env.getUnTillDbVCS().createBranch(null, rbUnTillDbFixedVer.getReleaseBranchName(), null);
		env.getUblVCS().createBranch(null, rbUBLFixedVer.getReleaseBranchName(), null);
		
		String unTillDbTagName = new DevelopBranch(compUnTillDb).getVersion().toPreviousMinorRelease();
		env.generateLogTag(env.getUnTillDbVCS(), rbUnTillDbFixedVer.getReleaseBranchName(), LogTag.SCM_BUILT);
		env.getUnTillDbVCS().createTag(rbUnTillDbFixedVer.getReleaseBranchName(), unTillDbTagName, "release " + unTillDbTagName);
		
		ReleaseBranchStatus rbs = rbUnTillFixedVer.getStatus();
		assertEquals(ReleaseBranchStatus.BRANCHED, rbs);
		
		String ublTagName = new DevelopBranch(compUBL).getVersion().toPreviousMinorRelease();
		env.generateLogTag(env.getUblVCS(), rbUBLFixedVer.getReleaseBranchName(), LogTag.SCM_BUILT);
		env.getUblVCS().createTag(rbUBLFixedVer.getReleaseBranchName(), ublTagName, "release " + ublTagName);
		
		rbs =  rbUnTillFixedVer.getStatus();
		assertEquals(ReleaseBranchStatus.MDEPS_TAGGED, rbs);
	}
	
	@Test
	public void testMDepsFrozen() {
//		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill, repos);
//		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb, repos);
//		ReleaseBranch rbUBL = new ReleaseBranch(compUBL, repos);
		
		env.getUnTillVCS().createBranch(null, rbUnTillFixedVer.getReleaseBranchName(), null);
		env.getUnTillDbVCS().createBranch(null, rbUnTillDbFixedVer.getReleaseBranchName(), null);
		env.getUblVCS().createBranch(null, rbUBLFixedVer.getReleaseBranchName(), null);
		 
		// ubl: freeze unTillDb mdep 
		Component compUntillDbVersioned = new Component(TestEnvironment.PRODUCT_UNTILLDB + ":" + env.getUnTillDbVer().toString(), repos);
		MDepsFile ublMDepsFile = new MDepsFile(Arrays.asList(compUntillDbVersioned));
		env.getUblVCS().setFileContent(rbUBLFixedVer.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, ublMDepsFile.toFileContent(), "mdeps versions frozen");
		
		ReleaseBranchStatus rbs = rbUnTillFixedVer.getStatus();
		assertEquals(ReleaseBranchStatus.BRANCHED, rbs);
		
		// unTill: freeze UBL mdep
		Component compUBLVersioned = new Component(TestEnvironment.PRODUCT_UBL+ ":" + env.getUblVer().toString(), repos);
		MDepsFile unTillMDepsFile = new MDepsFile(Arrays.asList(compUBLVersioned, compUnTillDb));
		env.getUnTillVCS().setFileContent(rbUnTillFixedVer.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, unTillMDepsFile.toFileContent(), "UBL mdep version frozen");
		
		rbs = rbUnTillFixedVer.getStatus();
		assertEquals(ReleaseBranchStatus.BRANCHED, rbs); // unTill: unTillDb still not frozen
		
		// unTill: freeze unTillDb mdep
		unTillMDepsFile = new MDepsFile(Arrays.asList(compUBLVersioned, compUntillDbVersioned));
		env.getUnTillVCS().setFileContent(rbUnTillFixedVer.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, unTillMDepsFile.toFileContent(), "unTillDb mdep version frozen");
		
		rbs = rbUnTillFixedVer.getStatus();
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbs); // unTill: unTillDb still not frozen
	}
}
