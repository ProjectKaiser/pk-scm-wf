package org.scm4j.releaser;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
public class WorkflowBuildTest extends WorkflowTestBase {
	
	@Test
	public void testBuildAllAndTestIGNOREDDev() throws Exception {
		IAction action = getActionTreeBuild(compUnTill);
		assertIsGoingToForkAndBuild(action, compUnTill);
		execAction(action);
		checkUnTillBuilt();
		
		// check nothing happens next time
		action = getActionTreeBuild(compUnTill);
		assertIsGoingToDoNothing(action, compUnTill);
		execAction(action);
		checkUnTillBuilt();

		// test IGNORED dev branch state
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(),
				LogTag.SCM_IGNORE + " ignored feature commit added");
		action = getActionTreeBuild(compUnTill);
		assertIsGoingToDoNothing(action);
	}

	@Test
	public void testBuildRootIfNestedIsBuiltAlready() throws Exception {
		// build unTillDb
		IAction action = getActionTreeBuild(compUnTillDb);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		execAction(action);
		checkUnTillDbBuilt();
		
		// build UBL
		action = getActionTreeBuild(compUBL);
		assertIsGoingToForkAndBuild(action, compUBL);
		assertIsGoingToDoNothing(action, compUnTillDb);
		execAction(action);
		checkUBLBuilt();
	}

	@Test
	public void testBuildRootAndChildIfAllForkedAlready() throws Exception {
		// fork unTillDb
		IAction action = getActionTreeFork(compUnTillDb);
		assertIsGoingToFork(action, compUnTillDb);
		execAction(action);
		checkUnTillDbForked();
		
		// fork UBL
		action = getActionTreeFork(compUBL);
		assertIsGoingToFork(action, compUBL);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD, null, compUnTillDb);
		execAction(action);
		checkUBLForked();
		
		assertTrue(TestBuilder.getBuilders().isEmpty());
		
		// build UBL and unTillDb
		action = getActionTreeBuild(compUBL);
		assertIsGoingToBuild(action, compUnTillDb);
		assertIsGoingToBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
		execAction(action);
		checkUBLBuilt();
	}
	
	@Test
	public void testBuildSingleComponentTwice() throws Exception {
		// fork and build unTillDb
		IAction action = getActionTreeBuild(compUnTillDb);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		execAction(action);
		checkUnTillDbBuilt();
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature commit added");
		
		// fork and build unTillDb next release
		action = getActionTreeBuild(compUnTillDb);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		execAction(action);
		checkUnTillDbBuilt(2);
	}
	
	@Test
	public void testBuildAllIfNestedForked() throws Exception {
		// fork unTillDb
		IAction action = getActionTreeFork(compUnTillDb);
		assertIsGoingToFork(action, compUnTillDb);
		execAction(action);
		checkUnTillDbForked();
		
		// unTillDb - build, unTill and UBl - fork and build
		action = getActionTreeBuild(compUnTill);
		assertIsGoingToForkAndBuild(action, compUnTill, compUBL);
		assertIsGoingToBuild(action, compUnTillDb);
		execAction(action);
		checkUnTillBuilt();
	}
	
	@Test
	public void testSkipBuildsOnFORKActionKind() throws Exception {
		// fork all
		IAction action = getActionTreeFork(compUnTill);
		assertIsGoingToForkAll(action);
		execAction(action);
		checkUnTillForked();

		// try to build with FORK target action kind. All builds should be skipped
		action = getActionTreeFork(compUnTill);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD_MDEPS, null, compUnTill, compUBL);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD, null, compUnTillDb);
	}
}