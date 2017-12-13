package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.conf.Component;

public class WorkflowPatchesTest extends WorkflowTestBase {

	@Test
	public void testPatches() throws Exception {
		IAction action = getActionTreeBuild(compUnTill);
		assertIsGoingToForkAndBuildAll(action);
		execAction(action);
		checkUnTillBuilt();

		// add feature to existing unTillDb release
		ReleaseBranch rb = ReleaseBranchFactory.getCRB(compUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), rb.getName(), "patch feature added");

		// build unTillDb patch
		Component compUnTillDbPatch = new Component(UNTILLDB + ":" + env.getUnTillDbVer().toRelease());
		action = getActionTreeBuild(compUnTillDbPatch);
		assertIsGoingToBuild(action, compUnTillDb);
		execAction(action);
		
		rb = ReleaseBranchFactory.getReleaseBranchPatch(compUnTillDbPatch);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch().toNextPatch(),
				rb.getVersion());
		ExtendedStatusBuilder builder = new ExtendedStatusBuilder();
		assertEquals(BuildStatus.DONE, builder.getAndCacheMinorStatus(compUnTillDbPatch).getStatus());

		// Existing unTill and UBL release branches should actualize its mdeps
		action = getActionTreeBuild(compUnTill.clone(env.getUnTillVer().toRelease()));
		assertIsGoingToBuild(action, compUBL, BuildStatus.ACTUALIZE_PATCHES);
		assertIsGoingToBuild(action, compUnTill, BuildStatus.BUILD_MDEPS);
		assertIsGoingToDoNothing(action, compUnTillDb);
		execAction(action);

		// check unTill uses new untillDb and UBL versions in existing unTill release branch.
		rb = ReleaseBranchFactory.getReleaseBranchPatch(compUnTill.clone(env.getUnTillVer().toRelease()));
		
		List<Component> mdeps = rb.getMDeps();
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
		IAction action = getActionTreeBuild(compUnTillDb);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		execAction(action);
		checkUnTillDbBuilt();

		// fork new unTillDb Release 2.60
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature added");
		action = getActionTreeBuild(compUnTillDb);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		execAction(action);
		checkUnTillDbBuilt(2);

		ReleaseBranch rb = ReleaseBranchFactory.getCRB(compUnTillDb);
		assertEquals(env.getUnTillDbVer().toNextMinor().toRelease(), rb.getVersion());

		// add feature for 2.59.1
		Component compToPatch = new Component(UNTILLDB + ":2.59.1");
		rb = ReleaseBranchFactory.getReleaseBranchPatch(compToPatch);
		env.generateFeatureCommit(env.getUnTillDbVCS(), rb.getName(), "2.59.1 feature merged");

		// build new unTillDb patch 2.59.1
		action = getActionTreeBuild(compToPatch);
		assertIsGoingToBuild(action, compUnTillDb);
		execAction(action);
		rb = ReleaseBranchFactory.getReleaseBranchPatch(compToPatch);
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toPreviousMinor().toNextPatch().toRelease(), rb.getVersion());
	}
}
