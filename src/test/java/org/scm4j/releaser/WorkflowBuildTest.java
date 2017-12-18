package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.exceptions.EBuildOnNotForkedRelease;

import static org.junit.Assert.*;
public class WorkflowBuildTest extends WorkflowTestBase {
	
	@Test
	public void testBuildAllAndTestIGNOREDDev() throws Exception {
		forkAndBuild(compUnTill);
		
		// check nothing happens next time
		IAction action = getAndExecActionTreeBuild(compUnTill);
		assertActionDoesNothing(action, compUnTill);
		checkUnTillBuilt();

		// test IGNORED dev branch state
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(),
				LogTag.SCM_IGNORE + " ignored feature commit added");
		action = getAndExecActionTreeBuild(compUnTill);
		assertActionDoesNothing(action);
	}

	@Test
	public void testBuildRootIfNestedIsBuiltAlready() throws Exception {
		// build unTillDb
		forkAndBuild(compUnTillDb);
		
		// fork UBL
		IAction action = getAndExecActionTreeFork(compUBL);
		assertActionDoesFork(action, compUBL);
		assertActionDoesNothing(action, compUnTillDb);
		checkUBLForked();
		
		// build UBL
		action = getAndExecActionTreeBuild(compUBL);
		assertActionDoesBuildBuild(action, compUBL);
		assertActionDoesNothing(action, compUnTillDb);
		checkUBLBuilt();
	}

	@Test
	public void testBuildRootAndChildIfAllForkedAlready() throws Exception {
		// fork unTillDb
		IAction action = getAndExecActionTreeFork(compUnTillDb);
		assertActionDoesFork(action, compUnTillDb);
		checkUnTillDbForked();
		
		// fork UBL
		action = getAndExecActionTreeFork(compUBL);
		assertActionDoesFork(action, compUBL);
		assertActionDoesNothing(action, BuildStatus.BUILD, null, compUnTillDb);
		checkUBLForked();
		
		assertTrue(TestBuilder.getBuilders().isEmpty());
		
		// build UBL and unTillDb
		action = getAndExecActionTreeBuild(compUBL);
		assertActionDoesBuildBuild(action, compUnTillDb);
		assertActionDoesBuildBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
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
		IAction action = getAndExecActionTreeFork(compUnTill);
		assertActionDoesForkAll(action);
		checkUnTillForked();

		// try to build with FORK target action kind. All builds should be skipped
		action = getAndExecActionTreeFork(compUnTill);
		assertActionDoesNothing(action, BuildStatus.BUILD_MDEPS, null, compUnTill, compUBL);
		assertActionDoesNothing(action, BuildStatus.BUILD, null, compUnTillDb);
	}
	
	@Test
	public void testBuildOnNotForkedComponent() {
		try {
			getAndExecActionTreeBuild(compUnTill);
			fail();
		} catch (EBuildOnNotForkedRelease e) {
			assertEquals(compUnTillDb, e.getComp());
		}
	}
}