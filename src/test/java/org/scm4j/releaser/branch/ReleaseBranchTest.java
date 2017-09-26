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
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;

public class ReleaseBranchTest extends WorkflowTestBase {

	@Test
	public void testCreate() {
		ReleaseBranch crb = new ReleaseBranch(compUnTillDb);
		assertEquals(compUnTillDb, crb.getComponent());
		assertEquals(compUnTillDb.getVcsRepository().getReleaseBranchPrefix()
				+ env.getUnTillDbVer().toPreviousMinor().getReleaseNoPatchString(), crb.getName());
		assertEquals(env.getUnTillDbVer().toPreviousMinor().toRelease(), crb.getVersion());
	}

	@Test
	public void testExists() throws Exception {
		ReleaseBranch crb = new ReleaseBranch(compUnTillDb);
		assertFalse(crb.exists());

		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(),
				"feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		action.execute(new NullProgress());

		crb = new ReleaseBranch(compUnTillDb);
		assertTrue(crb.exists());
		assertEquals(compUnTillDb.getVcsRepository().getReleaseBranchPrefix()
				+ env.getUnTillDbVer().getReleaseNoPatchString(), crb.getName());
	}

	@Test
	public void testGetVersions() {
		ReleaseBranch crb = new ReleaseBranch(compUnTillDb);
		assertFalse(env.getUnTillDbVer().getPatch().equals("0"));
		assertEquals(env.getUnTillDbVer().toPreviousMinor().toRelease(), crb.getVersion());

		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(),
				"feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		action.execute(new NullProgress());

		crb = new ReleaseBranch(compUnTillDb);
		assertEquals(env.getUnTillDbVer().toRelease(), crb.getVersion());
		assertEquals(env.getUnTillDbVer().toRelease(), crb.getHeadVersion());
	}
	
	@Test
	public void testGetMDeps() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(new NullProgress());
		
		ReleaseBranch crb = new ReleaseBranch(compUnTill);
		List<Component> mDeps = crb.getMDeps();
		assertTrue(mDeps.size() == 2);
		assertTrue(mDeps.contains(compUBL.cloneWithDifferentVersion(env.getUblVer().toRelease())));
		assertTrue(mDeps.contains(compUnTillDb.cloneWithDifferentVersion(env.getUnTillDbVer().toRelease())));
	}
}
