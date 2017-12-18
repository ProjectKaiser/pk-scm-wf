package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.IAction;

public class WorkflowForkTest extends WorkflowTestBase {
	
	@Test
	public void testForkAll() throws Exception {
		IAction action = getAndExecActionTreeFork(compUnTill);
		assertActionDoesForkAll(action);
		execAction(action);
		checkUnTillForked();
		assertFalse(action.getClass().getMethod("toString").getDeclaringClass().equals(Object.class));
		
		// check nothing happens on next fork
		action = getAndExecActionTreeFork(compUnTill);
		assertIsGoingToSkipAll(action);
		execAction(action);
		checkUnTillForked();
	}
	
	@Test
	public void testForkRootOnly() throws Exception {
		forkAndBuild(compUnTill);

		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevelopBranch(), "feature added");
		// fork untill only
		IAction action = getAndExecActionTreeFork(compUnTill);
		assertActionDoesFork(action, compUnTill);
		assertActionDoesNothing(action, compUnTillDb, compUBL);
		execAction(action);
		checkUnTillOnlyForked(2);

		Version latestVersionUBL = getCrbNextVersion(compUBL);
		Version latestVersionUnTill = getCrbNextVersion(compUnTill);
		Version latestVersionUnTillDb = getCrbNextVersion(compUnTillDb);
		
		assertEquals(env.getUblVer().toReleaseZeroPatch().toNextPatch(), latestVersionUBL);
		assertEquals(env.getUnTillVer().toReleaseZeroPatch().toNextMinor(), latestVersionUnTill);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), latestVersionUnTillDb);

		// build untill only
		action = getAndExecActionTreeBuild(compUnTill);
		assertActionDoesBuildBuild(action, compUnTill);
		assertActionDoesNothing(action, compUnTillDb, compUBL);
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
		IAction action  = getAndExecActionTreeFork(compUBL);
		assertActionDoesFork(action, compUBL);
		assertActionDoesNothing(action, compUnTillDb);
		execAction(action);
		checkUBLForked(2);
		
		action = getAndExecActionTreeBuild(compUBL);
		assertActionDoesBuildBuild(action, compUBL);
		assertActionDoesNothing(action, compUnTillDb);
		execAction(action);
		checkUBLBuilt(2);
	}
}

