package org.scm4j.releaser;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.scmactions.SCMActionBuild;
public class WorkflowBuildTest extends WorkflowTestBase {
	
	private final SCMReleaser releaser = new SCMReleaser();
	
	@Test
	public void testBuildAllAndTestIGNOREDDev() throws Exception {
		// fork unTill
		IAction action = releaser.getActionTree(UNTILL);
		assertIsGoingToForkAll(action);
		action.execute(getProgress(action));
		checkUnTillForked();

		// build unTill
		action = releaser.getActionTree(UNTILL);
		assertIsGoingToBuildAll(action);
		action.execute(getProgress(action));
		checkUnTillBuilt();
		
		// test IGNORED dev branch state
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), LogTag.SCM_IGNORE + " ignored feature commit added");
	}

	@Test
	public void testBuildRootIfNestedIsBuiltAlready() throws Exception {
		// fork unTillDb 
		IAction action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		
		// build unTillDb
		action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		
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
	public void testBuildRootAndChildIfAllForkedAlready() throws Exception {
		// fork unTillDb
		IAction action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		
		// fork UBL
		action = releaser.getActionTree(UBL);
		assertIsGoingToFork(action, compUBL);
		assertThat(action, allOf(
				instanceOf(ActionNone.class),
				hasProperty("mbs", nullValue())), compUnTillDb);
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
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked();
		
		// build unTillDb
		action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbBuilt();
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature commit added");
		
		// fork unTillDb next release
		action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked(2);
		
		// build unTillDb next release
		action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToBuild(action, compUnTillDb);
		TestBuilder.getBuilders().clear();
		action.execute(getProgress(action));
		checkUnTillDbBuilt(2);
	}
	
	@Test
	public void testSkipBuildsIfParentUnforked() throws Exception {
		// fork unTillDb
		IAction action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked();

		// fork unTill. unTillDb build must be skipped
		action = releaser.getActionTree(UNTILL);
		assertIsGoingToFork(action, compUnTill, compUBL);
		assertThat(action, allOf(
				instanceOf(ActionNone.class),
				hasProperty("mbs", nullValue())), compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillForked();
		
		// check all is going to build
		action = releaser.getActionTree(UNTILL);
		assertIsGoingToBuildAll(action);
	}
	
	@Test
	public void testBuildPatchOnExistingRelease() throws Exception {
		// fork unTillDb 2.59
		IAction action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbForked();
		
		// build unTillDb 2.59.0
		action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		checkUnTillDbBuilt();
		
		// fork new unTillDb Release 2.60
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		
		// build new unTillDbRelease 2.60.0
		action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		
		assertEquals(env.getUnTillDbVer().toNextMinor().toRelease(), new ReleaseBranch(compUnTillDb).getVersion());
		
		// add feature for 2.59.1
		Component comp = new Component(UNTILLDB + ":2.59.1");
		ReleaseBranch rb = new ReleaseBranch(compUnTillDb, comp.getVersion());
		env.generateFeatureCommit(env.getUnTillDbVCS(), rb.getName(), "2.59.1 feature merged");
		
		// build new unTillDb patch
		action = releaser.getActionTree(comp);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toPreviousMinor().toNextPatch().toRelease(), new ReleaseBranch(comp, comp.getVersion()).getVersion());
	}

	@Test
	public void testSkipBuildsOnFORKActionKind() throws Exception {
		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		assertIsGoingToForkAll(action);
		action.execute(getProgress(action));
		checkUnTillForked();

		// try to build with FORK target action kind. All builds should be skipped
		action = releaser.getActionTree(compUnTill, ActionKind.FORK);
		assertThat(action, instanceOf(ActionNone.class), compUnTillDb, compUnTill, compUBL);
	}

	@Test
	public void testSkipChildForkIfParentGoingToBuild() throws Exception {
		// fork UBL
		IAction action = releaser.getActionTree(UBL);
		assertIsGoingToFork(action, compUBL);
		action.execute(getProgress(action));
		checkUBLForked();

		// build UBL
		action = releaser.getActionTree(UBL);
		assertIsGoingToBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
		action.execute(getProgress(action));
		checkUBLBuilt();

		// fork next UBL version
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		action = releaser.getActionTree(UBL);
		assertIsGoingToFork(action, compUBL);
		action.execute(getProgress(action));

		// generate unTillDb fork conditions
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");

		// ensure UnTillDb is going to fork
		action = releaser.getActionTree(UNTILLDB);
		assertIsGoingToFork(action, compUnTillDb);

		// build UBL. unTillDb fork should be skipped
		action = releaser.getActionTree(UBL);
		assertIsGoingToBuild(action, compUBL);
		assertThat(action, instanceOf(ActionNone.class), compUnTillDb);
	}

	@Test
	public void testUseMDepsFromExistingReleaseBranch() throws Exception {
		// fork UBL
		IAction action = releaser.getActionTree(UBL);
		assertIsGoingToFork(action, compUBL);
		action.execute(getProgress(action));
		checkUBLForked();

		action = releaser.getActionTree(UBL);
		assertIsGoingToBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
		// change mdeps in trunk. Ensure mdeps of release branch are used in action tree
		Component newComp = new Component("new-comp:12.13.14");
		MDepsFile mdf = new MDepsFile(Arrays.asList(newComp));
		env.getUblVCS().setFileContent(compUBL.getVcsRepository().getDevBranch(), SCMReleaser.MDEPS_FILE_NAME, mdf.toFileContent(),
				"using new mdeps in trunk");
		action = releaser.getActionTree(UBL);
		assertThat(action, allOf(
				instanceOf(SCMActionBuild.class),
				hasProperty("mbs", equalTo(BuildStatus.BUILD_MDEPS))), compUBL);
		assertThat(action, allOf(
				instanceOf(SCMActionBuild.class),
				hasProperty("mbs", equalTo(BuildStatus.BUILD))), compUnTillDb);
	}
}