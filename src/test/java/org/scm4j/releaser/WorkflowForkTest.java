package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.IAction;

public class WorkflowForkTest extends WorkflowTestBase {
	
	@Test
	public void testForkAll() throws Exception {
		IAction action = getActionTreeFork(compUnTill);
		assertIsGoingToForkAll(action);
		execAction(action);
		checkUnTillForked();
		assertFalse(action.getClass().getMethod("toString").getDeclaringClass().equals(Object.class));
		
		// check nothing happens on next fork
		action = getActionTreeFork(compUnTill);
		assertIsGoingToSkipAll(action);
		execAction(action);
		checkUnTillForked();
	}
	
	@Test
	public void testForkRootOnly() throws Exception {
		IAction action = getActionTreeBuild(compUnTill);
		assertIsGoingToForkAndBuildAll(action);
		execAction(action);
		checkUnTillBuilt();

		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevelopBranch(), "feature added");
		// fork untill only
		action = getActionTreeFork(compUnTill);
		assertIsGoingToFork(action, compUnTill);
		assertIsGoingToDoNothing(action, compUnTillDb, compUBL);
		execAction(action);
		checkUnTillOnlyForked(2);

		Version latestVersionUBL = getCrbNextVersion(compUBL);
		Version latestVersionUnTill = getCrbNextVersion(compUnTill);
		Version latestVersionUnTillDb = getCrbNextVersion(compUnTillDb);
		
		assertEquals(env.getUblVer().toReleaseZeroPatch().toNextPatch(), latestVersionUBL);
		assertEquals(env.getUnTillVer().toReleaseZeroPatch().toNextMinor(), latestVersionUnTill);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), latestVersionUnTillDb);

		// build untill only
		action = getActionTreeBuild(compUnTill);
		assertIsGoingToBuild(action, compUnTill);
		assertIsGoingToDoNothing(action, compUnTillDb, compUBL);
		execAction(action);

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
		IAction action = getActionTreeBuild(compUBL);
		execAction(action);

		// next fork unTillDb
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature added");
		action = getActionTreeFork(compUnTillDb);
		assertIsGoingToFork(action, compUnTillDb);
		execAction(action);
		checkUnTillDbForked(2);

		// UBL should be forked and built then
		action = getActionTreeBuild(compUBL);
		assertIsGoingToForkAndBuild(action, compUBL);
		assertIsGoingToBuild(action, compUnTillDb);
		execAction(action);
		checkUBLBuilt(2);
	}
}

