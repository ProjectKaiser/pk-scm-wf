package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.releaser.actions.IAction;

import static org.junit.Assert.*;

public class CurrentReleaseBranchTest extends WorkflowTestBase {

	@Test
	public void testCreate() {
		CurrentReleaseBranch crb = new CurrentReleaseBranch(compUnTillDb);
		assertEquals(compUnTillDb, crb.getComponent());
	}
	
	@Test
	public void testExists() throws Exception {
		CurrentReleaseBranch crb = new CurrentReleaseBranch(compUnTillDb);
		assertEquals(compUnTillDb.getVcsRepository().getReleaseBranchPrefix() +
				env.getUnTillDbVer().getReleaseNoPatchString(), crb.getName());
		assertFalse(crb.exists());
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		action.execute(new NullProgress());
		
		assertTrue(crb.exists());
		assertEquals(compUnTillDb.getVcsRepository().getReleaseBranchPrefix() +
				env.getUnTillDbVer().getReleaseNoPatchString(), crb.getName());
	}

	@Test
	public void testGetVersions() {
		CurrentReleaseBranch crb = new CurrentReleaseBranch(compUnTillDb);
		assertFalse(env.getUnTillDbVer().toRelease().getPatch().equals("0"));
		assertEquals(env.getUnTillDbVer().toPreviousMinor().setPatch("0").toRelease(), crb.getVersion());
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		action.execute(new NullProgress());

		crb = new CurrentReleaseBranch(compUnTillDb);
		assertEquals(env.getUnTillDbVer().toRelease().setPatch("0"), crb.getVersion());
		assertEquals(env.getUnTillDbVer().toRelease().setPatch("0"), crb.getHeadVersion());
	}




	
	
}
