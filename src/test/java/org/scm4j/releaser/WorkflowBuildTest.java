package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.scmactions.SCMActionBuild;
import org.scm4j.releaser.scmactions.SCMActionFork;

public class WorkflowBuildTest extends WorkflowTestBase {
	
	private SCMReleaser releaser = new SCMReleaser();
	
	@Test
	public void testBuildAllAndTestIGNOREDDev() {
		// fork unTill
		IAction action = releaser.getActionTree(UNTILL);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, SCMActionFork.class);
		exp.put(UNTILL, SCMActionFork.class);
		exp.put(UBL, SCMActionFork.class);
		checkChildActionsTypes(action, exp);
		action.execute(getProgress(action));
		checkUnTillForked();

		// build unTill
		action = releaser.getActionTree(UNTILL);
		exp = new Expectations();
		exp.put(UNTILLDB, SCMActionBuild.class);
		exp.put(UNTILL, SCMActionBuild.class);
		exp.put(UBL, SCMActionBuild.class);
		checkChildActionsTypes(action, exp);
		action.execute(getProgress(action));
		checkUnTillBuilt();
		
		// test IGNORED dev branch state
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), LogTag.SCM_IGNORE + " ignored feature commit added");
		action = releaser.getActionTree(UNTILL);
		exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UNTILL, ActionNone.class);
		exp.put(UBL, ActionNone.class);
		checkChildActionsTypes(action, exp);
	}

	@Test
	public void testBuildRootIfNestedIsBuiltAlready() throws Exception {
		// fork unTillDb 
		IAction action = releaser.getActionTree(UNTILLDB);
		action.execute(getProgress(action));
		
		// build unTillDb
		action = releaser.getActionTree(UNTILLDB);
		action.execute(getProgress(action));
		
		// fork UBL
		action = releaser.getActionTree(UBL);
		Expectations exp = new Expectations();
		exp.put(UBL, SCMActionFork.class);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		action.execute(getProgress(action));
		checkUBLForked();
		
		// build UBL
		action = releaser.getActionTree(UBL);
		exp = new Expectations();
		exp.put(UBL, SCMActionBuild.class);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		action.execute(getProgress(action));
		checkUBLBuilt();
	}

	@Test
	public void testBuildRootAndChildIfAllForkedAlready() throws Exception {
		// fork unTillDb
		IAction action = releaser.getActionTree(UNTILLDB);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, SCMActionFork.class);
		checkChildActionsTypes(action, exp);
		action.execute(getProgress(action));
		
		// fork UBL
		action = releaser.getActionTree(UBL);
		exp = new Expectations();
		exp.put(UBL, SCMActionFork.class);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		action.execute(getProgress(action));
		checkUBLForked();
		
		assertTrue(TestBuilder.getBuilders().isEmpty());
		
		// build UBL and unTillDb
		action = releaser.getActionTree(UBL);
		exp = new Expectations();
		exp.put(UBL, SCMActionBuild.class);
		exp.put(UNTILLDB, SCMActionBuild.class);
		checkChildActionsTypes(action, exp);
		
		action.execute(getProgress(action));
		checkUBLBuilt();
	}
	
	@Test
	public void testBuildSingleComponentTwice() throws Exception {
		// fork unTillDb
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(UNTILLDB);
		action.execute(getProgress(action));
		checkUnTillDbForked();
		
		// build unTillDb
		action = releaser.getActionTree(UNTILLDB);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, SCMActionBuild.class);
		checkChildActionsTypes(action, exp);
		assertTrue(TestBuilder.getBuilders().isEmpty());
		action.execute(getProgress(action));
		checkUnTillDbBuilt();
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature commit added");
		
		// fork unTillDb next release
		releaser = new SCMReleaser();
		action = releaser.getActionTree(UNTILLDB);
		action.execute(getProgress(action));
		checkUnTillDbForked(2);
		
		// build unTillDb next release
		action = releaser.getActionTree(UNTILLDB);
		exp = new Expectations();
		exp.put(UNTILLDB, SCMActionBuild.class);
		checkChildActionsTypes(action, exp);
		TestBuilder.getBuilders().clear();
		action.execute(getProgress(action));
		checkUnTillDbBuilt(2);
		
	}
	
	@Test
	public void testSkipBuildsIfParentUnforked() throws Exception {
		// fork unTillDb
		IAction action = releaser.getActionTree(UNTILLDB);
		action.execute(getProgress(action));
		checkUnTillDbForked();

		ReleaseBranch rbUBL = new ReleaseBranch(compUBL);
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill);

		assertFalse(env.getUnTillVCS().getBranches("").contains(rbUnTill.getName()));
		assertTrue(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDb.getName()));
		assertFalse(env.getUblVCS().getBranches("").contains(rbUBL.getName()));
		
		// fork unTill. unTillDb build must be skipped
		action = releaser.getActionTree(UNTILL);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UNTILL, SCMActionFork.class);
		exp.put(UBL, SCMActionFork.class);
		checkChildActionsTypes(action, exp);
		action.execute(getProgress(action));
		checkUnTillForked();
		
		// check all is going to build
		action = releaser.getActionTree(UNTILL);
		exp = new Expectations();
		exp.put(UNTILLDB, SCMActionBuild.class);
		exp.put(UNTILL, SCMActionBuild.class);
		exp.put(UBL, SCMActionBuild.class);
		checkChildActionsTypes(action, exp);
	}
	
	@Test
	public void testBuildPatchOnExistingRelease() throws Exception {
		// fork unTillDb 2.59
		IAction action = releaser.getActionTree(UNTILLDB);
		action.execute(getProgress(action));
		checkUnTillDbForked();
		
		// build unTillDb 2.59.0
		action = releaser.getActionTree(UNTILLDB);
		action.execute(getProgress(action));
		checkUnTillDbBuilt();
		
		// fork new unTillDb Release 2.60
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		action = releaser.getActionTree(UNTILLDB);
		action.execute(getProgress(action));
		
		// build new unTillDbRelease 2.60.0
		action = releaser.getActionTree(UNTILLDB);
		action.execute(getProgress(action));
		
		assertEquals(env.getUnTillDbVer().toNextMinor().toRelease(), new ReleaseBranch(compUnTillDb).getVersion());
		
		// add feature for 2.59.1
		Component comp = new Component(UNTILLDB + ":2.59.1");
		ReleaseBranch rb = new ReleaseBranch(compUnTillDb, comp.getVersion());
		env.generateFeatureCommit(env.getUnTillDbVCS(), rb.getName(), "2.59.1 feature merged");
		
		// build new unTillDb patch
		action = releaser.getActionTree(comp);
		action.execute(getProgress(action));
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toPreviousMinor().toNextPatch().toRelease(), new ReleaseBranch(comp, comp.getVersion()).getVersion());
	}
}