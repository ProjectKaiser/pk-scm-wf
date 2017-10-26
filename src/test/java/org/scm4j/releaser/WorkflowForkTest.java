package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;

import java.util.List;

import static org.junit.Assert.*;

public class WorkflowForkTest extends WorkflowTestBase {
	
	private final SCMReleaser releaser = new SCMReleaser();

	@Test
	public void testForkAndBuildRootOnly() throws Exception {
		// fork all
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
	public void testForkAll() throws Exception {
		IAction action = releaser.getActionTree(UNTILL, ActionKind.FORK_ONLY);
		assertIsGoingToForkAll(action);
		action.execute(getProgress(action));
		checkUnTillForked();
	}

	@Test
	public void testPatches() throws Exception {
		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		assertIsGoingToForkAndBuildAll(action);
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
		assertIsGoingToDoNothing(action, compUnTillDb);
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
	
}
