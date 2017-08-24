package org.scm4j.wf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.wf.branch.ReleaseBranchStatus;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.MDepsFile;

public class ReleaseBranchTest extends SCMWorkflowTestBase {
	
	@Test
	public void testMissing() {
		assertTrue(rbUnTillDbFixedVer.getStatus() == ReleaseBranchStatus.MISSING);
	}
	
	@Test
	public void testBranchedIfNoMDeps() throws Exception {
		env.getUnTillDbVCS().createBranch(null, rbUnTillDbFixedVer.getReleaseBranchName(), null);
		assertTrue(rbUnTillDbFixedVer.getStatus() == ReleaseBranchStatus.BRANCHED);
	}
	
	@Test
	public void testBranched() throws Exception {
		assertFalse (rbUnTillFixedVer.getStatus() == ReleaseBranchStatus.BRANCHED);
		
		env.getUnTillVCS().createBranch(null, rbUnTillFixedVer.getReleaseBranchName(), null);
		assertTrue (rbUnTillFixedVer.getStatus() == ReleaseBranchStatus.BRANCHED);
	}
	
	@Test
	public void testMDepsFrozen() {
		assertEquals(ReleaseBranchStatus.MISSING, rbUnTillFixedVer.getStatus());
		env.getUnTillVCS().createBranch(null, rbUnTillFixedVer.getReleaseBranchName(), null);
		env.getUnTillDbVCS().createBranch(null, rbUnTillDbFixedVer.getReleaseBranchName(), null);
		env.getUblVCS().createBranch(null, rbUBLFixedVer.getReleaseBranchName(), null);
		assertEquals(ReleaseBranchStatus.BRANCHED, rbUnTillFixedVer.getStatus());
		 
		Component compUntillDbVersioned = ublFreezeUnTillDb();
		
		assertEquals(ReleaseBranchStatus.BRANCHED, rbUnTillFixedVer.getStatus());
		
		MDepsFile unTillMDepsFile;
		Component compUBLVersioned = unTillFreezeUBL();
		
		assertEquals(ReleaseBranchStatus.BRANCHED, rbUnTillFixedVer.getStatus()); // unTill: unTillDb still not frozen
		
		// unTill: freeze unTillDb mdep
		unTillMDepsFile = new MDepsFile(Arrays.asList(compUBLVersioned, compUntillDbVersioned));
		env.getUnTillVCS().setFileContent(rbUnTillFixedVer.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, unTillMDepsFile.toFileContent(), "unTillDb mdep version frozen");
		
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUnTillFixedVer.getStatus());
	}
	
	@Test
	public void testMDepsActual() {
		assertEquals(ReleaseBranchStatus.MISSING, rbUnTillFixedVer.getStatus());
		env.getUnTillVCS().createBranch(null, rbUnTillFixedVer.getReleaseBranchName(), null);
		env.getUnTillDbVCS().createBranch(null, rbUnTillDbFixedVer.getReleaseBranchName(), null);
		env.getUblVCS().createBranch(null, rbUBLFixedVer.getReleaseBranchName(), null);
		assertEquals(ReleaseBranchStatus.BRANCHED, rbUnTillFixedVer.getStatus());
		 
		unTillFreezeMDeps();
	
		tagUBLAndUnTillDb();
		
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillFixedVer.getStatus());
		
		// make new unTillDb patch - MDeps should not be actual anymore. Need to actualize mdeps of root component.
		VCSCommit featureUnTillDbCommit = env.getUnTillDbVCS().setFileContent(rbUnTillDbFixedVer.getReleaseBranchName(), "feature file", "feature line", "feature added");
		env.getUnTillDbVCS().setFileContent(rbUnTillDbFixedVer.getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME, env.getUnTillDbVer().toNextPatch().toNextPatch().toReleaseString(), LogTag.SCM_VER);
		env.getUnTillDbVCS().createTag(rbUnTillDbFixedVer.getReleaseBranchName(), env.getUnTillDbVer().toNextPatch().toReleaseString(), "right tag", featureUnTillDbCommit.getRevision());
		
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUnTillFixedVer.getStatus());
	}

	private VCSCommit unTillFreezeMDeps() {
		Component compUntillDbVersioned = ublFreezeUnTillDb();
		
		MDepsFile unTillMDepsFile;
		Component compUBLVersioned = unTillFreezeUBL();
		
		// unTill: freeze unTillDb mdep
		unTillMDepsFile = new MDepsFile(Arrays.asList(compUBLVersioned, compUntillDbVersioned));
		return env.getUnTillVCS().setFileContent(rbUnTillFixedVer.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, unTillMDepsFile.toFileContent(), "unTillDb mdep version frozen");
	}
	
	@Test
	public void testActual() {
		assertEquals(ReleaseBranchStatus.MISSING, rbUnTillFixedVer.getStatus());
		env.getUnTillVCS().createBranch(null, rbUnTillFixedVer.getReleaseBranchName(), null);
		env.getUnTillDbVCS().createBranch(null, rbUnTillDbFixedVer.getReleaseBranchName(), null);
		env.getUblVCS().createBranch(null, rbUBLFixedVer.getReleaseBranchName(), null);
		assertEquals(ReleaseBranchStatus.BRANCHED, rbUnTillFixedVer.getStatus());
		 
	
		VCSCommit unTillCommitToReleaseOn = unTillFreezeMDeps();
		tagUBLAndUnTillDb();
		
		// make root release.
		env.getUnTillVCS().createTag(rbUnTillFixedVer.getReleaseBranchName(), env.getUnTillVer().toReleaseString(), "right tag", unTillCommitToReleaseOn.getRevision());
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillFixedVer.getStatus());
		
		env.getUnTillVCS().setFileContent(rbUnTillFixedVer.getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME, env.getUnTillVer().toNextPatch().toReleaseString(), LogTag.SCM_VER);
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUnTillFixedVer.getStatus());
	}

	private void tagUBLAndUnTillDb() {
		VCSCommit unTillDbVerCommit = env.getUnTillDbVCS().setFileContent(rbUnTillDbFixedVer.getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME, env.getUnTillDbVer().toReleaseString(), LogTag.SCM_VER);
		VCSCommit ublVerCommit = env.getUblVCS().setFileContent(rbUBLFixedVer.getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME, env.getUblVer().toReleaseString(), LogTag.SCM_VER);
		env.getUnTillDbVCS().setFileContent(rbUnTillDbFixedVer.getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME, env.getUnTillDbVer().toNextPatch().toReleaseString(), LogTag.SCM_VER);
		env.getUblVCS().setFileContent(rbUBLFixedVer.getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME, env.getUblVer().toNextPatch().toReleaseString(), LogTag.SCM_VER);
		env.getUnTillDbVCS().createTag(rbUnTillDbFixedVer.getReleaseBranchName(), env.getUnTillDbVer().toReleaseString(), "right tag", unTillDbVerCommit.getRevision());
		env.getUblVCS().createTag(rbUBLFixedVer.getReleaseBranchName(), env.getUblVer().toReleaseString(), "right tag", ublVerCommit.getRevision());
	}

	private Component unTillFreezeUBL() {
		Component compUBLVersioned = new Component(TestEnvironment.PRODUCT_UBL+ ":" + env.getUblVer().toString(), repos);
		MDepsFile unTillMDepsFile = new MDepsFile(Arrays.asList(compUBLVersioned, compUnTillDb));
		env.getUnTillVCS().setFileContent(rbUnTillFixedVer.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, unTillMDepsFile.toFileContent(), "UBL mdep version frozen");
		return compUBLVersioned;
	}

	private Component ublFreezeUnTillDb() {
		Component compUntillDbVersioned = new Component(TestEnvironment.PRODUCT_UNTILLDB + ":" + env.getUnTillDbVer().toString(), repos);
		MDepsFile ublMDepsFile = new MDepsFile(Arrays.asList(compUntillDbVersioned));
		env.getUblVCS().setFileContent(rbUBLFixedVer.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, ublMDepsFile.toFileContent(), "unTillDb mdep version frozen");
		return compUntillDbVersioned;
	}
	
}
