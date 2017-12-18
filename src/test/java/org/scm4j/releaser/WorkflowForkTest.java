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
		forkAndBuild(compUnTill);

		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevelopBranch(), "feature added");
		// fork untill only
		IAction action = getActionTreeFork(compUnTill);
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
		forkAndBuild(compUBL);

		// next fork unTillDb
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature added");
		forkAndBuild(compUnTillDb, 2);

		// UBL should be forked and built then
		IAction action  = getActionTreeFork(compUBL);
		assertIsGoingToFork(action, compUBL);
		assertIsGoingToDoNothing(action, compUnTillDb);
		execAction(action);
		checkUBLForked(2);
		
		action = getActionTreeBuild(compUBL);
		assertIsGoingToBuild(action, compUBL);
		assertIsGoingToDoNothing(action, compUnTillDb);
		execAction(action);
		checkUBLBuilt(2);
	}
}

