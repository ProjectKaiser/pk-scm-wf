package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.scm4j.releaser.actions.IAction;

public class CurrentReleaseBranchTest extends WorkflowTestBase {
	
	@Test
	public void testGetDevVersion() {
		CurrentReleaseBranch crb = new CurrentReleaseBranch(compUnTillDb);
		assertEquals(env.getUnTillDbVer(), crb.getDevVersion());
	}
	
	@Test
	public void testExists() {
		CurrentReleaseBranch crb = new CurrentReleaseBranch(compUnTillDb);
		assertFalse(crb.exists());
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getProductionReleaseAction(compUnTillDb);
		action.execute(new NullProgress());
		
		assertTrue(crb.exists());
		assertEquals(env.getUnTillDbVer().toNextMinor(), crb.getDevVersion());
		assertEquals(env.getUnTillDbVer().toRelease(), crb.getVersion());
	}
	
	
}
