package org.scm4j.releaser.branch;

import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ReleaseBranchTest extends WorkflowTestBase {

	private final SCMReleaser releaser = new SCMReleaser();

	@Test
	public void testCreate() {
		ReleaseBranch rb = new ReleaseBranch(compUnTillDb);
		assertEquals(compUnTillDb, rb.getComponent());
		assertEquals(compUnTillDb.getVcsRepository().getReleaseBranchPrefix()
				+ env.getUnTillDbVer().toPreviousMinor().getReleaseNoPatchString(), rb.getName());
	}

	@Test
	public void testExists() throws Exception {
		ReleaseBranch rb = new ReleaseBranch(compUnTillDb);
		assertFalse(rb.exists());

		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(),
				"feature added");
		IAction action = releaser.getActionTree(compUnTillDb);
		action.execute(getProgress(action));

		rb = new ReleaseBranch(compUnTillDb);
		assertTrue(rb.exists());
		assertEquals(compUnTillDb.getVcsRepository().getReleaseBranchPrefix()
				+ env.getUnTillDbVer().getReleaseNoPatchString(), rb.getName());
	}

	@Test
	public void testGetMDeps() throws Exception {
		IAction action = releaser.getActionTree(compUnTill, ActionKind.FORK_ONLY);
		action.execute(getProgress(action));
		
		ReleaseBranch rb = new ReleaseBranch(compUnTill);
		List<Component> mDeps = rb.getMDeps();
		assertTrue(mDeps.size() == 2);
		assertTrue(mDeps.contains(compUBL.clone(env.getUblVer().toReleaseZeroPatch())));
		assertTrue(mDeps.contains(compUnTillDb.clone(env.getUnTillDbVer().toReleaseZeroPatch())));
	}

	@Test
	public void testVersionSelect() throws Exception {
		assertEquals(env.getUnTillDbVer().toPreviousMinor().toReleaseZeroPatch(), new ReleaseBranch(compUnTillDb).getVersion());
		Version testVer = new Version("11.12");
		assertEquals(testVer, new ReleaseBranch(compUnTillDb, testVer).getVersion());

		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature added");

		IAction action = releaser.getActionTree(compUnTill, ActionKind.FORK_ONLY);
		assertIsGoingToForkAll(action);
		action.execute(getProgress(action));

		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch(), new ReleaseBranch(compUnTillDb).getVersion());
	}

	@Test
	public void testMDepsUsageIfNewBranches() throws Exception {
		// fork UBL
		IAction action = releaser.getActionTree(UBL);
		assertIsGoingToForkAndBuild(action, compUBL, compUnTillDb);
		action.execute(getProgress(action));
		checkUBLBuilt();

		// fork next UBL version
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevelopBranch(), "feature added");
		action = releaser.getActionTree(UBL, ActionKind.FORK_ONLY);
		assertIsGoingToFork(action, compUBL);
		action.execute(getProgress(action));

		// generate unTillDb fork conditions
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature added");

		// ensure UnTillDb is going to fork
		action = releaser.getActionTree(UNTILLDB, ActionKind.FORK_ONLY);
		assertIsGoingToFork(action, compUnTillDb);

		// build UBL. unTillDb fork should be skipped
		action = releaser.getActionTree(UBL);
		assertIsGoingToBuild(action, compUBL);
		assertIsGoingToDoNothing(action, compUnTillDb);
	}

	@Test
	public void testMDepsUsageIfNewMDeps() throws Exception {
		// fork UBL
		IAction action = releaser.getActionTree(UBL, ActionKind.FORK_ONLY);
		assertIsGoingToFork(action, compUBL, compUnTillDb);
		action.execute(getProgress(action));
		checkUBLForked();

		action = releaser.getActionTree(UBL);
		assertIsGoingToBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
		// change mdeps in trunk. Ensure mdeps of release branch are used in action tree
		Component newComp = new Component("new-comp:12.13.14");
		MDepsFile mdf = new MDepsFile(Arrays.asList(newComp));
		env.getUblVCS().setFileContent(compUBL.getVcsRepository().getDevelopBranch(), SCMReleaser.MDEPS_FILE_NAME, mdf.toFileContent(),
				"using new mdeps in trunk");
		action = releaser.getActionTree(UBL);
		assertIsGoingToBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
		assertIsGoingToBuild(action, compUnTillDb, BuildStatus.BUILD);
	}
	
	@Test
	public void testToString() {
		assertNotNull(new ReleaseBranch(compUnTill).toString());
	}
}
