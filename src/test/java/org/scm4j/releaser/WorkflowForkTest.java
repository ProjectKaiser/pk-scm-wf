package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranchCurrent;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class WorkflowForkTest extends WorkflowTestBase {
	
	@Test
	public void testForkAll() throws Exception {
		IAction action = execAndGetActionFork(compUnTill);
		assertActionDoesForkAll(action);
		checkUnTillForked();
		
		// check nothing happens on next fork
		action = execAndGetActionFork(compUnTill);
		assertActionDoesSkipAll(action);
		checkUnTillForked();
	}
	
	@Test
	public void testForkRootOnly() throws Exception {
		forkAndBuild(compUnTill);

		env.generateFeatureCommit(env.getUnTillVCS(), repoUnTill.getDevelopBranch(), "feature added");
		// fork untill only
		IAction action = execAndGetActionFork(compUnTill);
		assertActionDoesFork(action, compUnTill);
		assertActionDoesNothing(action, compUnTillDb, compUBL);
		checkUnTillOnlyForked(2);

		Version latestVersionUBL = getCrbNextVersion(compUBL);
		Version latestVersionUnTill = getCrbNextVersion(compUnTill);
		Version latestVersionUnTillDb = getCrbNextVersion(compUnTillDb);
		
		assertEquals(env.getUblVer().toReleaseZeroPatch().toNextPatch(), latestVersionUBL);
		assertEquals(env.getUnTillVer().toReleaseZeroPatch().toNextMinor(), latestVersionUnTill);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), latestVersionUnTillDb);

		// build untill only
		action = execAndGetActionBuild(compUnTill);
		assertActionDoesBuild(action, compUnTill);
		assertActionDoesNothing(action, compUnTillDb, compUBL);

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
		forkAndBuild(compUBL);

		// next fork unTillDb
		env.generateFeatureCommit(env.getUnTillDbVCS(), repoUnTillDb.getDevelopBranch(), "feature added");
		forkAndBuild(compUnTillDb, 2);

		// UBL should be forked and built then
		IAction action  = execAndGetActionFork(compUBL);
		assertActionDoesFork(action, compUBL);
		assertActionDoesNothing(action, compUnTillDb);
		checkUBLForked(2);
		
		action = execAndGetActionBuild(compUBL);
		assertActionDoesBuild(action, compUBL);
		assertActionDoesNothing(action, compUnTillDb);
		checkUBLBuilt(2);
	}
	
	@Test
	public void testLockMDepsIfNotLocked() {
		fork(compUBL);
		
		// simulate mdeps not locked
		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUBL);
		MDepsFile mdf = new MDepsFile(env.getUblVCS().getFileContent(crb.getName(), Utils.MDEPS_FILE_NAME, null));
		mdf.replaceMDep(mdf.getMDeps().get(0).clone(""));
		env.getUblVCS().setFileContent(crb.getName(), Utils.MDEPS_FILE_NAME, mdf.toFileContent(), "mdeps not locked");
		
		// UBL should lock its mdeps
		IAction action = execAndGetActionFork(compUBL);
		assertThatAction(action, allOf(
				hasProperty("bsFrom", equalTo(BuildStatus.LOCK)),
				hasProperty("bsTo", equalTo(BuildStatus.LOCK))), compUBL);
		
		// check UBL mdeps locked
		mdf = new MDepsFile(env.getUblVCS().getFileContent(crb.getName(), Utils.MDEPS_FILE_NAME, null));
		for (Component mdep : mdf.getMDeps()) {
			assertTrue(mdep.getVersion().isLocked());
		}
	}

	@Test
	public void testForkRootIfNestedNotDone() throws Exception {
		forkAndBuild(compUBL);

		env.generateFeatureCommit(env.getUnTillDbVCS(), repoUnTillDb.getDevelopBranch(), "unTillDb feature added");

		IAction action = execAndGetActionFork(compUBL);
		assertActionDoesFork(action, compUBL, compUnTillDb);
		checkUBLForked(2);
	}
}

