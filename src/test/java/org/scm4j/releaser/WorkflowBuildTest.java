package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;

import static org.junit.Assert.*;
public class WorkflowBuildTest extends WorkflowTestBase {
	
	private final SCMReleaser releaser = new SCMReleaser();
	
	@Test
	public void testBuildAllAndTestIGNOREDDev() throws Exception {
		IAction action = releaser.getActionTree(UNTILL);
		assertIsGoingToForkAndBuild(action, compUnTill);
		action.execute(getProgress(action));
		checkUnTillBuilt();

		// test IGNORED dev branch state
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(),
				LogTag.SCM_IGNORE + " ignored feature commit added");
		action = releaser.getActionTree(UNTILL);
		assertIsGoingToDoNothing(action);
	}

	@Test
	public void testBuildRootIfNestedIsBuiltAlready() throws Exception {
		// build unTillDb
		IAction action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbBuilt();
		
		// build UBL
		action = releaser.getActionTree(UBL);
		assertIsGoingToForkAndBuild(action, compUBL);
		assertIsGoingToDoNothing(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUBLBuilt();
	}

	@Test
	public void testBuildRootAndChildIfAllForkedAlready() throws Exception {
		// fork unTillDb
		IAction action = releaser.getActionTree(UNTILLDB, ActionSet.FORK_ONLY);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked();
		
		// fork UBL
		action = releaser.getActionTree(UBL, ActionSet.FORK_ONLY);
		assertIsGoingToFork(action, compUBL);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD, null, compUnTillDb);
		action.execute(getProgress(action));
		checkUBLForked();
		
		assertTrue(TestBuilder.getBuilders().isEmpty());
		
		// build UBL and unTillDb
		action = releaser.getActionTree(UBL);
		assertIsGoingToBuild(action, compUnTillDb);
		assertIsGoingToBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
		action.execute(getProgress(action));
		checkUBLBuilt();
	}
	
	@Test
	public void testBuildSingleComponentTwice() throws Exception {
		// fork unTillDb
		IAction action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbBuilt();
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature commit added");
		
		// fork unTillDb next release
		action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbBuilt(2);
	}
	
	@Test
	public void testBuildAllIfNestedForked() throws Exception {
		// fork unTillDb
		IAction action = releaser.getActionTree(UNTILLDB, ActionSet.FORK_ONLY);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked();
		
		// unTillDb - build, unTill and UBl - fork and build
		action = releaser.getActionTree(UNTILL);
		assertIsGoingToForkAndBuild(action, compUnTill, compUBL);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillBuilt();
	}
	
	@Test
	public void testSkipBuildsOnFORKActionKind() throws Exception {
		// fork all
		IAction action = releaser.getActionTree(compUnTill, ActionSet.FORK_ONLY);
		assertIsGoingToForkAll(action);
		action.execute(getProgress(action));
		checkUnTillForked();

		// try to build with FORK target action kind. All builds should be skipped
		action = releaser.getActionTree(compUnTill, ActionSet.FORK_ONLY);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD_MDEPS, null, compUnTill, compUBL);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD, null, compUnTillDb);
	}
}