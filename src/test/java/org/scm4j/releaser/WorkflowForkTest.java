package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.IAction;

public class WorkflowForkTest extends WorkflowTestBase {
	
	private final ActionTreeBuilder actionBuilder = new ActionTreeBuilder();
	
	@Test
	public void testForkAll() throws Exception {
		IAction action = actionBuilder.getActionTree(UNTILL);
		assertIsGoingToForkAndBuildAll(action);
		action.execute(getProgress(action));
		checkUnTillBuilt();
		assertFalse(action.getClass().getMethod("toString").getDeclaringClass().equals(Object.class));
	}

	@Test
	public void testForkRootOnly() throws Exception {
		IAction action = actionBuilder.getActionTree(UNTILL);
		assertIsGoingToForkAndBuildAll(action);
		action.execute(getProgress(action));
		checkUnTillBuilt();

		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevelopBranch(), "feature added");
		// fork untill only
		action = actionBuilder.getActionTreeForkOnly(UNTILL);
		assertIsGoingToFork(action, compUnTill);
		assertIsGoingToDoNothing(action, compUnTillDb, compUBL);
		action.execute(getProgress(action));
		checkUnTillOnlyForked(2);

		Version latestVersionUBL = getCrbNextVersion(compUBL);
		Version latestVersionUnTill = getCrbNextVersion(compUnTill);
		Version latestVersionUnTillDb = getCrbNextVersion(compUnTillDb);
		
		assertEquals(env.getUblVer().toReleaseZeroPatch().toNextPatch(), latestVersionUBL);
		assertEquals(env.getUnTillVer().toReleaseZeroPatch().toNextMinor(), latestVersionUnTill);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), latestVersionUnTillDb);

		// build untill only
		action = actionBuilder.getActionTree(UNTILL);
		assertIsGoingToBuild(action, compUnTill);
		assertIsGoingToDoNothing(action, compUnTillDb, compUBL);
		action.execute(getProgress(action));

		latestVersionUBL = getCrbNextVersion(compUBL);
		latestVersionUnTill = getCrbNextVersion(compUnTill);
		latestVersionUnTillDb = getCrbNextVersion(compUnTillDb);
		assertEquals(env.getUblVer().toReleaseZeroPatch().toNextPatch(), latestVersionUBL);
		assertEquals(env.getUnTillVer().toReleaseZeroPatch().toNextMinor().toNextPatch(), latestVersionUnTill);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), latestVersionUnTillDb);
		assertEquals(env.getUblVer().toNextMinor(), dbUBL.getVersion());
		assertEquals(env.getUnTillVer().toNextMinor().toNextMinor(), dbUnTill.getVersion());
		assertEquals(env.getUnTillDbVer().toNextMinor(), dbUnTillDb.getVersion());
	}

	@Test
	public void testForkRootIfNestedIsForkedAlready() throws Exception {
		// build UBL + unTillDb
		IAction action = actionBuilder.getActionTree(UBL);
		action.execute(getProgress(action));

		// next fork unTillDb
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature added");
		action = actionBuilder.getActionTreeForkOnly(compUnTillDb);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked(2);

		// UBL should be forked then
		action = actionBuilder.getActionTree(compUBL);
		assertIsGoingToForkAndBuild(action, compUBL);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUBLBuilt(2);
	}
}

