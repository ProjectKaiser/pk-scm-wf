package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;

import static org.junit.Assert.*;

public class WorkflowForkTest extends WorkflowTestBase {
	
	private final SCMReleaser releaser = new SCMReleaser();

	@Test
	public void testForkRootOnly() throws Exception {
		IAction action = releaser.getActionTree(UNTILL);
		assertIsGoingToForkAndBuildAll(action);
		action.execute(getProgress(action));
		checkUnTillBuilt();

		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevelopBranch(), "feature added");
		// fork untill only
		action = releaser.getActionTree(UNTILL, ActionKind.FORK_ONLY);
		assertIsGoingToFork(action, compUnTill);
		assertIsGoingToDoNothing(action, compUnTillDb, compUBL);
		action.execute(getProgress(action));
		checkUnTillOnlyForked(2);

		ReleaseBranch rbUBL = new ReleaseBranch(compUBL);
		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill);
		ReleaseBranch rbUnTillDb= new ReleaseBranch(compUnTillDb);
		assertEquals(env.getUblVer().toReleaseZeroPatch().toNextPatch(), rbUBL.getVersion());
		assertEquals(env.getUnTillVer().toReleaseZeroPatch().toNextMinor(), rbUnTill.getVersion());
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), rbUnTillDb.getVersion());

		// build untill only
		action = releaser.getActionTree(UNTILL);
		assertIsGoingToBuild(action, compUnTill);
		assertIsGoingToDoNothing(action, compUnTillDb, compUBL);
		action.execute(getProgress(action));

		rbUBL = new ReleaseBranch(compUBL);
		rbUnTill = new ReleaseBranch(compUnTill);
		rbUnTillDb= new ReleaseBranch(compUnTillDb);
		assertEquals(env.getUblVer().toReleaseZeroPatch().toNextPatch(), rbUBL.getVersion());
		assertEquals(env.getUnTillVer().toReleaseZeroPatch().toNextMinor().toNextPatch(), rbUnTill.getVersion());
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), rbUnTillDb.getVersion());
		assertEquals(env.getUblVer().toNextMinor(), dbUBL.getVersion());
		assertEquals(env.getUnTillVer().toNextMinor().toNextMinor(), dbUnTill.getVersion());
		assertEquals(env.getUnTillDbVer().toNextMinor(), dbUnTillDb.getVersion());
	}

	@Test
	public void testForkRootIfNestedIsForkedAlready() throws Exception {
		// build UBL + unTillDb
		IAction action = releaser.getActionTree(UBL);
		action.execute(getProgress(action));

		// second fork unTillDb
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature added");
		action = releaser.getActionTree(compUnTillDb, ActionKind.FORK_ONLY);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked(2);

		// UBL should be forked then
		action = releaser.getActionTree(compUBL, ActionKind.FORK_ONLY);
		assertIsGoingToFork(action, compUBL);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD, null, compUnTillDb);
		action.execute(getProgress(action));
		checkUBLForked(2);
	}
}
