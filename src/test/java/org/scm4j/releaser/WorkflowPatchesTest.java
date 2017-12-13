package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.MDepsSource;
import org.scm4j.releaser.conf.Component;

public class WorkflowPatchesTest extends WorkflowTestBase {

	private final SCMReleaser releaser = new SCMReleaser();

	@Test
	public void testPatches() throws Exception {
		IAction action = releaser.getActionTree(compUnTill);
		assertIsGoingToForkAndBuildAll(action);
		action.execute(getProgress(action));
		checkUnTillBuilt();

		// add feature to existing unTillDb release
		MDepsSource mDepsSource = new MDepsSource(compUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), mDepsSource.getCrbName(), "patch feature added");

		// build unTillDb patch
		Component compUnTillDbPatch = new Component(UNTILLDB + ":" + env.getUnTillDbVer().toRelease());
		action = releaser.getActionTree(compUnTillDbPatch);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		mDepsSource = new MDepsSource(compUnTillDbPatch, true);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch().toNextPatch(),
				mDepsSource.getCrbVersion());
		ExtendedStatusTreeBuilder builder = new ExtendedStatusTreeBuilder();
		assertEquals(BuildStatus.DONE, builder.getExtendedStatusTreeNode(compUnTillDbPatch).getStatus());

		// Existing unTill and UBL release branches should actualize its mdeps
		action = releaser.getActionTree(compUnTill.clone(env.getUnTillVer().toRelease()));
		assertIsGoingToBuild(action, compUBL, BuildStatus.ACTUALIZE_PATCHES);
		assertIsGoingToBuild(action, compUnTill, BuildStatus.BUILD_MDEPS);
		assertIsGoingToDoNothing(action, compUnTillDb);
		action.execute(getProgress(action));

		// check unTill uses new untillDb and UBL versions in existing unTill release branch.
		mDepsSource = new MDepsSource(compUnTill.clone(env.getUnTillVer().toRelease()), true);
		
		List<Component> mdeps = mDepsSource.getMDeps();
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
	public void testBuildPatchOnExistingRelease() throws Exception {
		// fork unTillDb 2.59
		IAction action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbBuilt();

		// fork new unTillDb Release 2.60
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature added");
		action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbBuilt(2);

		MDepsSource mDepsSource = new MDepsSource(compUnTillDb);
		assertEquals(env.getUnTillDbVer().toNextMinor().toRelease(), mDepsSource.getCrbVersion());

		// add feature for 2.59.1
		Component compToPatch = new Component(UNTILLDB + ":2.59.1");
		mDepsSource = new MDepsSource(compToPatch);
		env.generateFeatureCommit(env.getUnTillDbVCS(), mDepsSource.getRbName(), "2.59.1 feature merged");

		// build new unTillDb patch 2.59.1
		action = releaser.getActionTree(compToPatch);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		mDepsSource = new MDepsSource(compToPatch, true);
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toPreviousMinor().toNextPatch().toRelease(), mDepsSource.getRbVersion());
	}
}
