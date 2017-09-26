package org.scm4j.releaser.branch;

import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.releaser.NullProgress;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.scmactions.SCMActionFork;

import java.util.List;

import static org.junit.Assert.*;

public class ReleaseBranchTest extends WorkflowTestBase {

	@Test
	public void testCreate() {
		ReleaseBranch rb = new ReleaseBranch(compUnTillDb);
		assertEquals(compUnTillDb, rb.getComponent());
		assertEquals(compUnTillDb.getVcsRepository().getReleaseBranchPrefix()
				+ env.getUnTillDbVer().toPreviousMinor().getReleaseNoPatchString(), rb.getName());
		assertEquals(env.getUnTillDbVer().toPreviousMinor().toRelease(), rb.getVersion());
	}

	@Test
	public void testExists() throws Exception {
		ReleaseBranch rb = new ReleaseBranch(compUnTillDb);
		assertFalse(rb.exists());

		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(),
				"feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		action.execute(new NullProgress());

		rb = new ReleaseBranch(compUnTillDb);
		assertTrue(rb.exists());
		assertEquals(compUnTillDb.getVcsRepository().getReleaseBranchPrefix()
				+ env.getUnTillDbVer().getReleaseNoPatchString(), rb.getName());
	}

	@Test
	public void testGetVersions() {
		ReleaseBranch rb = new ReleaseBranch(compUnTillDb);
		assertFalse(env.getUnTillDbVer().getPatch().equals("0"));
		assertEquals(env.getUnTillDbVer().toPreviousMinor().toRelease(), rb.getVersion());

		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(),
				"feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		action.execute(new NullProgress());

		rb = new ReleaseBranch(compUnTillDb);
		assertEquals(env.getUnTillDbVer().toRelease(), rb.getVersion());
		assertEquals(env.getUnTillDbVer().toRelease(), rb.getVersion());
	}
	
	@Test
	public void testGetMDeps() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(new NullProgress());
		
		ReleaseBranch rb = new ReleaseBranch(compUnTill);
		List<Component> mDeps = rb.getMDeps();
		assertTrue(mDeps.size() == 2);
		assertTrue(mDeps.contains(compUBL.cloneWithDifferentVersion(env.getUblVer().toRelease())));
		assertTrue(mDeps.contains(compUnTillDb.cloneWithDifferentVersion(env.getUnTillDbVer().toRelease())));
	}

	@Test
	public void testVersionSelect() {
		assertEquals(env.getUnTillDbVer().toPreviousMinor().toRelease(), new ReleaseBranch(compUnTillDb).getVersion());
		Version testVer = new Version("11.12");
		assertEquals(testVer, new ReleaseBranch(compUnTillDb, testVer).getVersion());

		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTill);
		assertTrue(action instanceof SCMActionFork);
		action.execute(new NullProgress());

		assertEquals(env.getUnTillDbVer().toPreviousMinor().toRelease().toNextPatch(), new ReleaseBranch(compUnTillDb).getVersion());
	}
}
