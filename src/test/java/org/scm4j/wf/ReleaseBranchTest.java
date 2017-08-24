package org.scm4j.wf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
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
	public void testTagged() {
		assertFalse(rbUnTillDbFixedVer.getStatus() == ReleaseBranchStatus.TAGGED);
		
		env.getUnTillDbVCS().createBranch(null, rbUnTillDbFixedVer.getReleaseBranchName(), null);
		assertFalse(rbUnTillDbFixedVer.getStatus() == ReleaseBranchStatus.TAGGED);
		
		VCSCommit commit = env.getUnTillDbVCS().setFileContent(rbUnTillDbFixedVer.getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME, env.getUnTillVer().toReleaseString(), LogTag.SCM_VER);
		assertFalse(rbUnTillDbFixedVer.getStatus() == ReleaseBranchStatus.TAGGED);
		
		env.getUnTillDbVCS().createTag(rbUnTillDbFixedVer.getReleaseBranchName(), "wrong_tag", "wrong tag", commit.getRevision());
		assertFalse(rbUnTillDbFixedVer.getStatus() == ReleaseBranchStatus.TAGGED);
		
		env.getUnTillDbVCS().setFileContent(rbUnTillDbFixedVer.getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME, env.getUnTillDbVer().toNextPatch().toReleaseString(), LogTag.SCM_VER);
		assertFalse(rbUnTillDbFixedVer.getStatus() == ReleaseBranchStatus.TAGGED);
		
		env.getUnTillDbVCS().createTag(rbUnTillDbFixedVer.getReleaseBranchName(), env.getUnTillDbVer().toReleaseString(), "wrong tag", commit.getRevision());
		assertTrue(rbUnTillDbFixedVer.getStatus() == ReleaseBranchStatus.TAGGED);
	}
	
	@Test
	public void testMDepsTagged () {
//		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill, repos);
//		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb, repos);
//		ReleaseBranch rbUBL = new ReleaseBranch(compUBL, repos);
		
		env.getUnTillVCS().createBranch(null, rbUnTillFixedVer.getReleaseBranchName(), null);
		env.getUnTillDbVCS().createBranch(null, rbUnTillDbFixedVer.getReleaseBranchName(), null);
		env.getUblVCS().createBranch(null, rbUBLFixedVer.getReleaseBranchName(), null);
		
		String unTillDbTagName = new DevelopBranch(compUnTillDb).getVersion().toPreviousMinor().toReleaseString();
		env.generateLogTag(env.getUnTillDbVCS(), rbUnTillDbFixedVer.getReleaseBranchName(), LogTag.SCM_BUILT);
		env.getUnTillDbVCS().createTag(rbUnTillDbFixedVer.getReleaseBranchName(), unTillDbTagName, "release " + unTillDbTagName, null);
		
		ReleaseBranchStatus rbs = rbUnTillFixedVer.getStatus();
		assertEquals(ReleaseBranchStatus.BRANCHED, rbs);
		
		String ublTagName = new DevelopBranch(compUBL).getVersion().toPreviousMinor().toReleaseString();
		env.generateLogTag(env.getUblVCS(), rbUBLFixedVer.getReleaseBranchName(), LogTag.SCM_BUILT);
		env.getUblVCS().createTag(rbUBLFixedVer.getReleaseBranchName(), ublTagName, "release " + ublTagName, null);
		
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
