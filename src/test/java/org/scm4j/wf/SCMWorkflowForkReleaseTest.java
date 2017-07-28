package org.scm4j.wf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.scmactions.ReleaseReason;
import org.scm4j.wf.scmactions.SCMActionForkReleaseBranch;

public class SCMWorkflowForkReleaseTest extends SCMWorkflowTestBase {
	
	@Test
	public void testForkUnTill() throws Exception {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILL);
		Expectations exp = new Expectations();
		exp.put(UNTILL, SCMActionForkReleaseBranch.class);
		exp.put(UNTILL, "reason", ReleaseReason.NEW_FEATURES);
		exp.put(UBL, ActionNone.class);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		// check branches
		assertTrue(env.getUnTillVCS().getBranches("").contains(rbUnTillFixedVer.getReleaseBranchName()));
		assertFalse(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDbFixedVer.getReleaseBranchName()));
		assertFalse(env.getUblVCS().getBranches("").contains(rbUBLFixedVer.getReleaseBranchName()));
		
		// check versions
		assertEquals(env.getUnTillVer().toNextMinorSnapshot(), dbUnTill.getVersion().toString());
		ReleaseBranch newUnTillRB = new ReleaseBranch(compUnTill, env.getUnTillVer(), repos);
		assertEquals(env.getUnTillVer(), newUnTillRB.getVersion());
		
		// check mDeps
		List<Component> unTillReleaseMDeps = rbUnTillFixedVer.getMDeps();
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component unTillReleaseMDep : unTillReleaseMDeps) {
			if (unTillReleaseMDep.getName().equals(UBL)) {
				assertEquals(dbUBL.getVersion().toPreviousMinorRelease(), unTillReleaseMDep.getVersion().toReleaseString());
			} else if (unTillReleaseMDep.getName().equals(UNTILLDB)) {
				assertEquals(dbUnTillDb.getVersion().toPreviousMinorRelease(), unTillReleaseMDep.getVersion().toReleaseString());
			} else {
				fail();
			}
		}
	}
	
	@Test
	public void testForkUBLAfterUnTillDb() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		
		// fork unTillDb
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// build unTillDb
		action = wf.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// fork UBL
		action = wf.getProductionReleaseAction(UBL);
		action.execute(new NullProgress());
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UBL, SCMActionForkReleaseBranch.class);
		exp.put(UBL,  "reason", ReleaseReason.NEW_DEPENDENCIES);
		checkChildActionsTypes(action, exp);
		action = wf.getProductionReleaseAction(UBL);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		// check branches
		assertFalse(env.getUnTillVCS().getBranches("").contains(rbUnTillFixedVer.getReleaseBranchName()));
		assertTrue(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDbFixedVer.getReleaseBranchName()));
		assertTrue(env.getUblVCS().getBranches("").contains(rbUBLFixedVer.getReleaseBranchName()));
		
		// check UBL versions
		assertEquals(env.getUblVer().toNextMinorSnapshot(), dbUBL.getVersion().toString());
		ReleaseBranch newUBLRB = new ReleaseBranch(compUBL, env.getUblVer(), repos);
		assertEquals(env.getUblVer(), newUBLRB.getVersion());
		
		// check unTillDb versions
		assertEquals(env.getUnTillDbVer().toNextMinorSnapshot(), dbUnTillDb.getVersion().toString());
		ReleaseBranch newUnTillDbRB = new ReleaseBranch(compUnTillDb, env.getUnTillDbVer(), repos);
		assertEquals(env.getUnTillDbVer(), newUnTillDbRB.getVersion());
		
		// check UBL mDeps
		List<Component> ublReleaseMDeps = rbUBLFixedVer.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		assertEquals(dbUnTillDb.getVersion().toPreviousMinorRelease(), ublReleaseMDeps.get(0).getVersion().toReleaseString());
	}

	@Test
	public void testForkUnTillAndUBL() throws Exception {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILL);
		Expectations exp = new Expectations();
		exp.put(UNTILL, SCMActionForkReleaseBranch.class);
		exp.put(UNTILL, "reason", ReleaseReason.NEW_FEATURES);
		exp.put(UBL, SCMActionForkReleaseBranch.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_FEATURES);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		assertTrue(env.getUnTillVCS().getBranches("").contains(rbUnTillFixedVer.getReleaseBranchName()));
		assertFalse(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDbFixedVer.getReleaseBranchName()));
		assertTrue(env.getUblVCS().getBranches("").contains(rbUBLFixedVer.getReleaseBranchName()));
		
		List<Component> unTillReleaseMDeps = rbUnTillFixedVer.getMDeps();
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component unTillReleaseMDep : unTillReleaseMDeps) {
			if (unTillReleaseMDep.getName().equals(UBL)) {
				assertEquals(dbUBL.getVersion().toPreviousMinorRelease(), unTillReleaseMDep.getVersion().toReleaseString());
			} else if (unTillReleaseMDep.getName().equals(UNTILLDB)) {
				assertEquals(dbUnTillDb.getVersion().toPreviousMinorRelease(), unTillReleaseMDep.getVersion().toReleaseString());
			} else {
				fail();
			}
		}
		
		List<Component> ublReleaseMDeps = rbUBLFixedVer.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(dbUnTillDb.getVersion().toPreviousMinorRelease(), ublReleaseMDeps.get(0).getVersion().toReleaseString());
	}
	
	@Test
	public void testForkUnTillAndUBLAndUnTillDb() throws Exception {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILL);
		Expectations exp = new Expectations();
		exp.put(UNTILL, SCMActionForkReleaseBranch.class);
		exp.put(UNTILL, "reason", ReleaseReason.NEW_FEATURES);
		exp.put(UBL, SCMActionForkReleaseBranch.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_FEATURES);
		exp.put(UNTILLDB, SCMActionForkReleaseBranch.class);
		exp.put(UNTILLDB, "reason", ReleaseReason.NEW_FEATURES);
		checkChildActionsTypes(action, exp);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		assertTrue(env.getUnTillVCS().getBranches("").contains(rbUnTillFixedVer.getReleaseBranchName()));
		assertTrue(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDbFixedVer.getReleaseBranchName()));
		assertTrue(env.getUblVCS().getBranches("").contains(rbUBLFixedVer.getReleaseBranchName()));
		
		List<Component> unTillReleaseMDeps = rbUnTillFixedVer.getMDeps();
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component unTillReleaseMDep : unTillReleaseMDeps) {
			if (unTillReleaseMDep.getName().equals(UBL)) {
				assertEquals(dbUBL.getVersion().toPreviousMinorRelease(), unTillReleaseMDep.getVersion().toReleaseString());
			} else if (unTillReleaseMDep.getName().equals(UNTILLDB)) {
				assertEquals(dbUnTillDb.getVersion().toPreviousMinorRelease(), unTillReleaseMDep.getVersion().toReleaseString());
			} else {
				fail();
			}
		}
		
		List<Component> ublReleaseMDeps = rbUBLFixedVer.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(dbUnTillDb.getVersion().toPreviousMinorRelease(), ublReleaseMDeps.get(0).getVersion().toReleaseString());
		
		assertTrue(rbUnTillDbFixedVer.getMDeps().isEmpty());
	}
	
	@Test
	public void testSkipBuildsIfParentUnforked() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, SCMActionForkReleaseBranch.class);
		checkChildActionsTypes(action, exp);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}

		assertFalse(env.getUnTillVCS().getBranches("").contains(rbUnTillFixedVer.getReleaseBranchName()));
		assertTrue(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDbFixedVer.getReleaseBranchName()));
		assertFalse(env.getUblVCS().getBranches("").contains(rbUBLFixedVer.getReleaseBranchName()));
		
		wf = new SCMWorkflow();
		action = wf.getProductionReleaseAction(UNTILL);
		exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UNTILL, SCMActionForkReleaseBranch.class);
		exp.put(UNTILL, "reason", ReleaseReason.NEW_DEPENDENCIES);
		exp.put(UBL, SCMActionForkReleaseBranch.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_DEPENDENCIES);
		checkChildActionsTypes(action, exp);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		assertTrue(env.getUnTillVCS().getBranches("").contains(rbUnTillFixedVer.getReleaseBranchName()));
		assertTrue(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDbFixedVer.getReleaseBranchName()));
		assertTrue(env.getUblVCS().getBranches("").contains(rbUBLFixedVer.getReleaseBranchName()));
	}
}
