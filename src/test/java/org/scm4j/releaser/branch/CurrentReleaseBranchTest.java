package org.scm4j.releaser.branch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.scm4j.releaser.NullProgress;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.CurrentReleaseBranch;
import org.scm4j.releaser.conf.Component;

public class CurrentReleaseBranchTest extends WorkflowTestBase {

	@Test
	public void testCreate() {
		CurrentReleaseBranch crb = new CurrentReleaseBranch(compUnTillDb);
		assertEquals(compUnTillDb, crb.getComponent());
		assertEquals(compUnTillDb.getVcsRepository().getReleaseBranchPrefix()
				+ env.getUnTillDbVer().toPreviousMinor().getReleaseNoPatchString(), crb.getName());
		assertEquals(env.getUnTillDbVer().toPreviousMinor().toRelease(), crb.getVersion());
	}

	@Test
	public void testExists() throws Exception {
		CurrentReleaseBranch crb = new CurrentReleaseBranch(compUnTillDb);
		assertFalse(crb.exists());

		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(),
				"feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		action.execute(new NullProgress());

		crb = new CurrentReleaseBranch(compUnTillDb);
		assertTrue(crb.exists());
		assertEquals(compUnTillDb.getVcsRepository().getReleaseBranchPrefix()
				+ env.getUnTillDbVer().getReleaseNoPatchString(), crb.getName());
	}

	@Test
	public void testGetVersions() {
		CurrentReleaseBranch crb = new CurrentReleaseBranch(compUnTillDb);
		assertFalse(env.getUnTillDbVer().getPatch().equals("0"));
		assertEquals(env.getUnTillDbVer().toPreviousMinor().toRelease(), crb.getVersion());

		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(),
				"feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		action.execute(new NullProgress());

		crb = new CurrentReleaseBranch(compUnTillDb);
		assertEquals(env.getUnTillDbVer().toRelease(), crb.getVersion());
		assertEquals(env.getUnTillDbVer().toRelease(), crb.getHeadVersion());
	}
	
	@Test
	public void testGetMDeps() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(new NullProgress());
		
		CurrentReleaseBranch crb = new CurrentReleaseBranch(compUnTill);
		List<Component> mDeps = crb.getMDeps();
		assertTrue(mDeps.size() == 2);
		assertTrue(mDeps.contains(compUBL.cloneWithDifferentVersion(env.getUblVer().toRelease())));
		assertTrue(mDeps.contains(compUnTillDb.cloneWithDifferentVersion(env.getUnTillDbVer().toRelease())));
	}
}
