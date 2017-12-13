package org.scm4j.releaser;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
public class WorkflowBuildTest extends WorkflowTestBase {
	
	private final ActionTreeBuilder actionBuilder = new ActionTreeBuilder();
	
	@Test
	public void testBuildAllAndTestIGNOREDDev() throws Exception {
		IAction action = actionBuilder.getActionTree(UNTILL);
		assertIsGoingToForkAndBuild(action, compUnTill);
		action.execute(getProgress(action));
		checkUnTillBuilt();

		// test IGNORED dev branch state
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(),
				LogTag.SCM_IGNORE + " ignored feature commit added");
		action = actionBuilder.getActionTree(UNTILL);
		assertIsGoingToDoNothing(action);
	}

	@Test
	public void testBuildRootIfNestedIsBuiltAlready() throws Exception {
		// build unTillDb
		IAction action = actionBuilder.getActionTree(UNTILLDB);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbBuilt();
		
		// build UBL
		action = actionBuilder.getActionTree(UBL);
		assertIsGoingToForkAndBuild(action, compUBL);
		assertIsGoingToDoNothing(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUBLBuilt();
	}

	@Test
	public void testBuildRootAndChildIfAllForkedAlready() throws Exception {
		// fork unTillDb
		IAction action = actionBuilder.getActionTreeForkOnly(UNTILLDB);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked();
		
		// fork UBL
		action = actionBuilder.getActionTreeForkOnly(UBL);
		assertIsGoingToFork(action, compUBL);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD, null, compUnTillDb);
		action.execute(getProgress(action));
		checkUBLForked();
		
		assertTrue(TestBuilder.getBuilders().isEmpty());
		
		// build UBL and unTillDb
		action = actionBuilder.getActionTree(UBL);
		assertIsGoingToBuild(action, compUnTillDb);
		assertIsGoingToBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
		action.execute(getProgress(action));
		checkUBLBuilt();
	}
	
	@Test
	public void testBuildSingleComponentTwice() throws Exception {
		// fork unTillDb
		IAction action = actionBuilder.getActionTree(UNTILLDB);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbBuilt();
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature commit added");
		
		// fork unTillDb next release
		action = actionBuilder.getActionTree(UNTILLDB);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbBuilt(2);
	}
	
	@Test
	public void testBuildAllIfNestedForked() throws Exception {
		// fork unTillDb
		IAction action = actionBuilder.getActionTreeForkOnly(UNTILLDB);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked();
		
		// unTillDb - build, unTill and UBl - fork and build
		action = actionBuilder.getActionTree(UNTILL);
		assertIsGoingToForkAndBuild(action, compUnTill, compUBL);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillBuilt();
	}
	
	@Test
	public void testSkipBuildsOnFORKActionKind() throws Exception {
		// fork all
		IAction action = actionBuilder.getActionTreeForkOnly(compUnTill);
		assertIsGoingToForkAll(action);
		action.execute(getProgress(action));
		checkUnTillForked();

		// try to build with FORK target action kind. All builds should be skipped
		action = actionBuilder.getActionTreeForkOnly(compUnTill);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD_MDEPS, null, compUnTill, compUBL);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD, null, compUnTillDb);
	}
}