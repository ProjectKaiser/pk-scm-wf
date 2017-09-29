package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.scmactions.SCMActionBuild;
import org.scm4j.releaser.scmactions.SCMActionFork;

public class WorkflowForkTest extends WorkflowTestBase {
	
	SCMReleaser releaser = new SCMReleaser();

	@Test
	public void testForkSingleComponentNoMDeps() throws Exception {
		IAction action = releaser.getActionTree(UNTILLDB);
		assertTrue(action instanceof SCMActionFork);
		action.execute(getProgress(action));
		checkUnTillDbForked();
	}
	
	@Test
	public void testForkRootOnly() throws Exception {
		// fork unTill
		IAction action = releaser.getActionTree(UNTILL);
		action.execute(getProgress(action));
		assertTrue(action instanceof SCMActionFork);
		checkUnTillForked();
		
		// build untill
		action = releaser.getActionTree(UNTILL);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(getProgress(action));
		checkUnTillBuilt();
		
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		// fork untill only
		action = releaser.getActionTree(UNTILL);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UNTILL, SCMActionFork.class);
		exp.put(UBL, ActionNone.class);
		checkChildActionsTypes(action, exp);
		action.execute(getProgress(action));
		
		ReleaseBranch rbUBL = new ReleaseBranch(compUBL);
		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill);
		ReleaseBranch rbUnTillDb= new ReleaseBranch(compUnTillDb);
		assertEquals(env.getUblVer().toReleaseZeroPatch().toNextPatch(), rbUBL.getVersion());
		assertEquals(env.getUnTillVer().toReleaseZeroPatch().toNextMinor(), rbUnTill.getVersion());
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), rbUnTillDb.getVersion());
		//checkUnTillForked();
		
		// build untill only
		action = releaser.getActionTree(UNTILL);
		exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UNTILL, SCMActionBuild.class);
		exp.put(UBL, ActionNone.class);
		checkChildActionsTypes(action, exp);
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
	public void testRootBuiltUsingNewPatchesIfNestedBuiltAlready() throws Exception {
		// fork unTillDb
		IAction action = releaser.getActionTree(UNTILLDB);
		action.execute(getProgress(action));
		checkUnTillDbForked();
		
		// build unTillDb
		action = releaser.getActionTree(UNTILLDB);
		action.execute(getProgress(action));
		checkUnTillDbBuilt();
		
		// fork UBL
		action = releaser.getActionTree(UBL);
		action.execute(getProgress(action));
		checkUBLForked();
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UBL, SCMActionFork.class);
		checkChildActionsTypes(action, exp);
		
		// build UBL
		action = releaser.getActionTree(UBL);
		exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UBL, SCMActionBuild.class);
		checkChildActionsTypes(action, exp);
		action.execute(getProgress(action));
		checkUBLBuilt();
	}

	@Test
	public void testForkAll() throws Exception {
		IAction action = releaser.getActionTree(UNTILL);
		Expectations exp = new Expectations();
		exp.put(UNTILL, SCMActionFork.class);
		exp.put(UBL, SCMActionFork.class);
		exp.put(UNTILLDB, SCMActionFork.class);
		checkChildActionsTypes(action, exp);
		
		action.execute(getProgress(action));
		checkUnTillForked();
	}

	@Test
	public void testPatches() throws Exception {
		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(getProgress(action));
		checkUnTillForked();

		// build all
		action = releaser.getActionTree(compUnTill);
		action.execute(getProgress(action));
		checkUnTillBuilt();

		// add feature to existing unTillDb release
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), rbUnTillDb.getName(), "patch feature added");
		
		// build unTillDb patch
		action = releaser.getActionTree(new Component(UNTILLDB + ":" + env.getUnTillDbVer().toRelease()));
		action.execute(getProgress(action));

		// Existing unTill and UBL release branches should actualize its mdeps first
		action = releaser.getActionTree(compUnTill.clone(env.getUnTillVer().toRelease()));
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UNTILL, SCMActionBuild.class);
		exp.put(UBL, SCMActionBuild.class);
		checkChildActionsTypes(action, exp);
		// actualize unTill and UBL mdeps
		action.execute(getProgress(action));

		// check unTill uses new untillDb and UBL versions in existing unTill release branch.
		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill.clone(env.getUnTillVer().toRelease()));
		List<Component> mdeps = rbUnTill.getMDeps();
		for (Component mdep : mdeps) {
			if (mdep.getName().equals(UBL)) {
				assertEquals(env.getUblVer().toReleaseZeroPatch().toNextPatch(), mdep.getVersion());
			} else if (mdep.getName().equals(UNTILLDB)) {
				assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), mdep.getVersion());
			} else {
				fail();
			}
		}
	}
	
	@Ignore // TODO: Are we need to use exact dev versions as is in release?
	@Test
	public void testUseExactDevVersionsAsIsInRelease() {
		Component compUnTillVersioned = new Component(UNTILL + ":" + env.getUnTillVer().toRelease());
		Component compUnTillDbVersioned = new Component(UNTILLDB + ":" + env.getUnTillDbVer().toRelease());
		Component compUblVersioned = new Component(UBL + ":" + env.getUblVer().toRelease());
		MDepsFile mDepsFile = new MDepsFile(Arrays.asList(compUnTillDbVersioned, compUblVersioned));
		env.getUnTillVCS().setFileContent(compUnTillVersioned.getVcsRepository().getDevBranch(), SCMReleaser.MDEPS_FILE_NAME, mDepsFile.toFileContent(), "-SNAPSHOT added to mDeps versions");
		SCMReleaser releaser = new SCMReleaser();

		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(getProgress(action));
		
		// check mDeps versions. All of them should be kept unchanged
		// UBL mDeps
		ReleaseBranch rbUBL = new ReleaseBranch(compUBL);
		List<Component> ublReleaseMDeps = rbUBL.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		assertEquals(env.getUnTillDbVer().toRelease(), ublReleaseMDeps.get(0).getVersion());
				
		
		// unTill mDeps
		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill);
		List<Component> unTillReleaseMDeps = rbUnTill.getMDeps();
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component mDep : unTillReleaseMDeps) {
			if (mDep.getName().equals(UBL)) {
				assertEquals(env.getUblVer().toRelease(), mDep.getVersion());
			} else if (mDep.getName().equals(UNTILLDB)) {
				assertEquals(env.getUnTillDbVer().toRelease(), mDep.getVersion());
			} else {
				fail();
			}
		}
	}
	
	@Test
	public void testActualizeExactSnapshotDevVersionsInRelease() {
		Component compUnTillVersioned = new Component(UNTILL + ":" + env.getUnTillVer().toSnapshot());
		Component compUnTillDbVersioned = new Component(UNTILLDB + ":" + env.getUnTillDbVer().toSnapshot());
		Component compUblVersioned = new Component(UBL + ":" + env.getUblVer().toSnapshot());
		MDepsFile mDepsFile = new MDepsFile(Arrays.asList(compUnTillDbVersioned, compUblVersioned));
		env.getUnTillVCS().setFileContent(compUnTillVersioned.getVcsRepository().getDevBranch(), SCMReleaser.MDEPS_FILE_NAME, mDepsFile.toFileContent(), "-SNAPSHOT added to mDeps versions");

		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(getProgress(action));
		
		// check mDeps versions. All of them should be replaced with new ones with no snapshot
		// UBL mDeps
		ReleaseBranch rbUBL = new ReleaseBranch(compUBL);
		List<Component> ublReleaseMDeps = rbUBL.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch(), ublReleaseMDeps.get(0).getVersion());
				
		
		// unTill mDeps
		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill);
		List<Component> unTillReleaseMDeps = rbUnTill.getMDeps();
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component mDep : unTillReleaseMDeps) {
			if (mDep.getName().equals(UBL)) {
				assertEquals(env.getUblVer().toReleaseZeroPatch(), mDep.getVersion());
			} else if (mDep.getName().equals(UNTILLDB)) {
				assertEquals(env.getUnTillDbVer().toReleaseZeroPatch(), mDep.getVersion());
			} else {
				fail();
			}
		}
	}
}
