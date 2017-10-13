package org.scm4j.releaser;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.scmactions.SCMActionFork;

public class WorkflowForkTest extends WorkflowTestBase {
	
	private final SCMReleaser releaser = new SCMReleaser();

	@Test
	public void testForkSingleComponentNoMDeps() throws Exception {
		IAction action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked();
	}
	
	@Test
	public void testForkRootOnly() throws Exception {
		// fork all
		IAction action = releaser.getActionTree(UNTILL);
		action.execute(getProgress(action));
		assertIsGoingToForkAll(action);
		checkUnTillForked();
		
		// build all
		action = releaser.getActionTree(UNTILL);
		assertIsGoingToBuildAll(action);
		action.execute(getProgress(action));
		checkUnTillBuilt();
		
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		// fork untill only
		action = releaser.getActionTree(UNTILL);
		assertThat(action, allOf(
				instanceOf(ActionNone.class),
				hasProperty("mbs", equalTo(BuildStatus.DONE))), compUnTillDb, compUBL);
		assertIsGoingToFork(action, compUnTill);
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
		assertThat(action, allOf(
				instanceOf(ActionNone.class),
				hasProperty("mbs", equalTo(BuildStatus.DONE))), compUnTillDb, compUBL);
		assertIsGoingToBuild(action, compUnTill);
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
	public void testBuildRootUsingNewPatchesIfNestedBuiltAlready() throws Exception {
		// fork unTillDb
		IAction action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked();
		
		// build unTillDb
		action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbBuilt();
		
		// fork UBL
		action = releaser.getActionTree(UBL);
		assertIsGoingToFork(action, compUBL);
		assertThat(action, allOf(
				instanceOf(ActionNone.class),
				hasProperty("mbs", equalTo(BuildStatus.DONE))), compUnTillDb);
		action.execute(getProgress(action));
		checkUBLForked();

		// build UBL
		action = releaser.getActionTree(UBL);
		assertIsGoingToBuild(action, compUBL);
		assertThat(action, allOf(
				instanceOf(ActionNone.class),
				hasProperty("mbs", equalTo(BuildStatus.DONE))), compUnTillDb);
		action.execute(getProgress(action));
		checkUBLBuilt();
	}

	@Test
	public void testForkAll() throws Exception {
		IAction action = releaser.getActionTree(UNTILL);
		assertIsGoingToForkAll(action);
		action.execute(getProgress(action));
		checkUnTillForked();
	}

	@Test
	public void testPatches() throws Exception {
		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		assertIsGoingToForkAll(action);
		action.execute(getProgress(action));
		checkUnTillForked();

		// build all
		action = releaser.getActionTree(compUnTill);
		assertIsGoingToBuildAll(action);
		action.execute(getProgress(action));
		checkUnTillBuilt();

		// add feature to existing unTillDb release
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), rbUnTillDb.getName(), "patch feature added");
		
		// build unTillDb patch
		Component compUnTillDbPatch = new Component(UNTILLDB + ":" + env.getUnTillDbVer().toRelease());
		action = releaser.getActionTree(compUnTillDbPatch);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		ReleaseBranch rbUnTillDbPatch = new ReleaseBranch(compUnTillDbPatch);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch().toNextPatch(),
				rbUnTillDbPatch.getVersion());
		assertEquals(BuildStatus.DONE, new Build(compUnTillDbPatch).getStatus());

		// Existing unTill and UBL release branches should actualize its mdeps
		action = releaser.getActionTree(compUnTill.clone(env.getUnTillVer().toRelease()));
		assertIsGoingToBuild(action, compUBL, BuildStatus.ACTUALIZE_PATCHES);
		assertIsGoingToBuild(action, compUnTill, BuildStatus.BUILD_MDEPS);
		assertThat(action, allOf(
				instanceOf(ActionNone.class),
				hasProperty("mbs", equalTo(BuildStatus.DONE))), compUnTillDb);
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
	
	@Test
	public void testSkipForksOnBUILDActionKind() {
		//try to fork all with BUILD target action kind. All forks should be skipped
		IAction action = releaser.getActionTree(compUnTill, ActionKind.BUILD);
		assertThat(action, allOf(
				instanceOf(ActionNone.class),
				hasProperty("mbs", equalTo(BuildStatus.FORK))), compUnTillDb, compUBL, compUnTill);
	}

	@Test
	public void testSkipChildBuildIfParentGoingToFork() {
		// fork UBL
		IAction action = releaser.getActionTree(UBL);
		assertIsGoingToFork(action, compUBL, compUnTillDb);
		action.execute(getProgress(action));
		checkUBLForked();

		// build UBL
		action = releaser.getActionTree(UBL);
		assertIsGoingToBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUBLBuilt();

		// fork next unTillDb version
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToFork(action, compUnTillDb);
		assertTrue(action instanceof SCMActionFork);
		action.execute(getProgress(action));

		// generate UBL fork conditions
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");

		// ensure UnTillDb is going to build
		action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToBuild(action, compUnTillDb);

		// fork UBL. unTillDb build should be skipped
		action = releaser.getActionTree(UBL);
		assertIsGoingToFork(action, compUBL);
		assertThat(action, instanceOf(ActionNone.class), compUnTillDb);
	}
}
