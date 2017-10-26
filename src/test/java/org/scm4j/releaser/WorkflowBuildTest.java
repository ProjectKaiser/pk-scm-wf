package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;

import java.util.Arrays;

import static org.junit.Assert.*;
public class WorkflowBuildTest extends WorkflowTestBase {
	
	private final SCMReleaser releaser = new SCMReleaser();
	
	@Test
	public void testBuildAllAndTestIGNOREDDev() throws Exception {
		// fork unTill
		IAction action = releaser.getActionTree(UNTILL);
		assertIsGoingToForkAndBuild(action, compUnTill);
		action.execute(getProgress(action));
		checkUnTillBuilt();

		// test IGNORED dev branch state
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), LogTag.SCM_IGNORE + " ignored feature commit added");
	}

	@Test
	public void testBuildRootIfNestedIsBuiltAlready() throws Exception {
		// fork unTillDb 
		IAction action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbBuilt();
		
		// fork UBL
		action = releaser.getActionTree(UBL);
		assertIsGoingToForkAndBuild(action, compUBL);
		assertIsGoingToDoNothing(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUBLBuilt();
	}

	@Test
	public void testBuildRootAndChildIfAllForkedAlready() throws Exception {
		// fork unTillDb
		IAction action = releaser.getActionTree(UNTILLDB, ActionKind.FORK_ONLY);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked();
		
		// fork UBL
		action = releaser.getActionTree(UBL, ActionKind.FORK_ONLY);
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
	public void testRootForkAndBuildIfNestedForked() throws Exception {
		// fork unTillDb
		IAction action = releaser.getActionTree(UNTILLDB, ActionKind.FORK_ONLY);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked();
		
		// unTillDb - skip, unTill and UBl - fork only
		action = releaser.getActionTree(UNTILL, ActionKind.FORK_ONLY);
		assertIsGoingToFork(action, compUnTill, compUBL);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD, null, compUnTillDb);
		
		// unTillDb - build, unTill and UBl - fork and build
		action = releaser.getActionTree(UNTILL);
		assertIsGoingToForkAndBuild(action, compUnTill, compUBL);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillBuilt();
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
		
		assertEquals(env.getUnTillDbVer().toNextMinor().toRelease(), new ReleaseBranch(compUnTillDb).getVersion());
		
		// add feature for 2.59.1
		Component compToPatch = new Component(UNTILLDB + ":2.59.1");
		ReleaseBranch rb = new ReleaseBranch(compUnTillDb, compToPatch.getVersion());
		env.generateFeatureCommit(env.getUnTillDbVCS(), rb.getName(), "2.59.1 feature merged");
		
		// build new unTillDb patch
		action = releaser.getActionTree(compToPatch);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toPreviousMinor().toNextPatch().toRelease(), new ReleaseBranch(compToPatch, compToPatch.getVersion()).getVersion());
	}

	@Test
	public void testSkipBuildsOnFORKActionKind() throws Exception {
		// fork all
		IAction action = releaser.getActionTree(compUnTill, ActionKind.FORK_ONLY);
		assertIsGoingToForkAll(action);
		action.execute(getProgress(action));
		checkUnTillForked();

		// try to build with FORK target action kind. All builds should be skipped
		action = releaser.getActionTree(compUnTill, ActionKind.FORK_ONLY);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD_MDEPS, null, compUnTill, compUBL);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD, null, compUnTillDb);
	}

	@Test
	public void testExistingReleaseBranchMDepsUsageNewBranches() throws Exception {
		// fork UBL
		IAction action = releaser.getActionTree(UBL);
		assertIsGoingToForkAndBuild(action, compUBL, compUnTillDb);
		action.execute(getProgress(action));
		checkUBLBuilt();

		// fork next UBL version
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevelopBranch(), "feature added");
		action = releaser.getActionTree(UBL, ActionKind.FORK_ONLY);
		assertIsGoingToFork(action, compUBL);
		action.execute(getProgress(action));

		// generate unTillDb fork conditions
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature added");

		// ensure UnTillDb is going to fork
		action = releaser.getActionTree(UNTILLDB, ActionKind.FORK_ONLY);
		assertIsGoingToFork(action, compUnTillDb);

		// build UBL. unTillDb fork should be skipped
		action = releaser.getActionTree(UBL);
		assertIsGoingToBuild(action, compUBL);
		assertIsGoingToDoNothing(action, compUnTillDb);
	}

	@Test
	public void testExistingReleaseBranchMDepsUsageNewMDeps() throws Exception {
		// fork UBL
		IAction action = releaser.getActionTree(UBL, ActionKind.FORK_ONLY);
		assertIsGoingToFork(action, compUBL, compUnTillDb);
		action.execute(getProgress(action));
		checkUBLForked();

		action = releaser.getActionTree(UBL);
		assertIsGoingToBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
		// change mdeps in trunk. Ensure mdeps of release branch are used in action tree
		Component newComp = new Component("new-comp:12.13.14");
		MDepsFile mdf = new MDepsFile(Arrays.asList(newComp));
		env.getUblVCS().setFileContent(compUBL.getVcsRepository().getDevelopBranch(), SCMReleaser.MDEPS_FILE_NAME, mdf.toFileContent(),
				"using new mdeps in trunk");
		action = releaser.getActionTree(UBL);
		assertIsGoingToBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
		assertIsGoingToBuild(action, compUnTillDb, BuildStatus.BUILD);
	}
}