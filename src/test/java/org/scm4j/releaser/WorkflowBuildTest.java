package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.exceptions.EBuildOnNotForkedRelease;
public class WorkflowBuildTest extends WorkflowTestBase {
	
	@Test
	public void testBuildAllAndTestIGNOREDDev() throws Exception {
		forkAndBuild(compUnTill);
		
		// check nothing happens next time
		IAction action = getActionTreeBuild(compUnTill);
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
		forkAndBuild(compUnTillDb);
		
		// fork UBL
		IAction action = getActionTreeFork(compUBL);
		assertIsGoingToFork(action, compUBL);
		assertIsGoingToDoNothing(action, compUnTillDb);
		execAction(action);
		checkUBLForked();
		
		// build UBL
		action = getActionTreeBuild(compUBL);
		assertIsGoingToBuild(action, compUBL);
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
		forkAndBuild(compUnTillDb);
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature commit added");

		forkAndBuild(compUnTillDb, 2);
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
	
	@Test
	public void testBuildOnNotForkedComponent() {
		try {
			getActionTreeBuild(compUnTill);
			fail();
		} catch (EBuildOnNotForkedRelease e) {
			assertEquals(compUnTillDb, e.getComp());
		}
	}
}