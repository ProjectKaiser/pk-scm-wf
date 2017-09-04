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
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.scmactions.ReleaseReason;
import org.scm4j.wf.scmactions.SCMActionForkReleaseBranch;

public class SCMWorkflowForkReleaseTest extends SCMWorkflowTestBase {
	
	@Test
	public void testForkSingleComponent() throws Exception {
		SCMWorkflow wf = new SCMWorkflow();
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		checkChildActionsTypes(action, exp);
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		exp = new Expectations();
		exp.put(UNTILLDB, SCMActionForkReleaseBranch.class);
		exp.put(UNTILLDB, "reason", ReleaseReason.NEW_FEATURES);
		action = wf.getProductionReleaseAction(UNTILLDB);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		// check branches
		assertTrue(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDbFixedVer.getName()));
		
		// check versions
		Version verTrunk = dbUnTillDb.getVersion();
		ReleaseBranch newUnTillDbRB = new ReleaseBranch(compUnTillDb, repos);
		Version verRelease = newUnTillDbRB.getCurrentVersion();
		assertEquals(env.getUnTillDbVer().toNextMinor(), verTrunk);
		assertEquals(env.getUnTillDbVer().toRelease(), verRelease);
	}
	
	@Test
	public void testForkRootComponentWithMDeps() throws Exception {
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
		assertTrue(env.getUnTillVCS().getBranches("").contains(rbUnTillFixedVer.getName()));
		assertFalse(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDbFixedVer.getName()));
		assertFalse(env.getUblVCS().getBranches("").contains(rbUBLFixedVer.getName()));
		
		// check versions
		Version verTrunk = dbUnTill.getVersion();
		ReleaseBranch newUnTillRB = new ReleaseBranch(compUnTill, repos);
		Version verRelease = newUnTillRB.getCurrentVersion();
		assertEquals(env.getUnTillVer().toNextMinor(), verTrunk);
		assertEquals(env.getUnTillVer().toRelease(), verRelease);
		
		// check mDeps
		List<Component> unTillReleaseMDeps = rbUnTillFixedVer.getMDeps();
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component unTillReleaseMDep : unTillReleaseMDeps) {
			if (unTillReleaseMDep.getName().equals(UBL)) {
				assertEquals(dbUBL.getVersion().toRelease(), unTillReleaseMDep.getVersion());
			} else if (unTillReleaseMDep.getName().equals(UNTILLDB)) {
				assertEquals(dbUnTillDb.getVersion().toRelease(), unTillReleaseMDep.getVersion());
			} else {
				fail();
			}
		}
	}
	
	@Test
	public void testForkRootIfNestedForkedAlready() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		
		// fork unTillDb
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// build unTillDb
		action = wf.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// fork UBL
		// simulate BRANCHED dev branch status
		env.generateContent(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "test file", "test content", LogTag.SCM_VER);
		action = wf.getProductionReleaseAction(UBL);
		action.execute(new NullProgress());
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UBL, SCMActionForkReleaseBranch.class);
		exp.put(UBL,  "reason", ReleaseReason.NEW_DEPENDENCIES);
		checkChildActionsTypes(action, exp);
		
		// build UBL
		action = wf.getProductionReleaseAction(UBL);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		// check branches
		assertFalse(env.getUnTillVCS().getBranches("").contains(rbUnTillFixedVer.getName()));
		assertTrue(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDbFixedVer.getName()));
		assertTrue(env.getUblVCS().getBranches("").contains(rbUBLFixedVer.getName()));
		
		// check UBL versions
		assertEquals(env.getUblVer().toNextMinor(), dbUBL.getVersion());
		ReleaseBranch newUBLRB = new ReleaseBranch(compUBL, repos);
		assertEquals(env.getUblVer().toNextPatch().toRelease(), newUBLRB.getCurrentVersion());
		
		// check unTillDb versions
		assertEquals(env.getUnTillDbVer().toNextMinor(), dbUnTillDb.getVersion());
		ReleaseBranch newUnTillDbRB = new ReleaseBranch(compUnTillDb, repos);
		assertEquals(env.getUnTillDbVer().toNextPatch().toRelease(), newUnTillDbRB.getCurrentVersion());
		
		// check UBL mDeps
		List<Component> ublReleaseMDeps = rbUBLFixedVer.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toRelease(), ublReleaseMDeps.get(0).getVersion());
	}

	@Test
	public void testForkRootWithNotAllMDeps() throws Exception {
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
		
		assertTrue(env.getUnTillVCS().getBranches("").contains(rbUnTillFixedVer.getName()));
		assertFalse(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDbFixedVer.getName()));
		assertTrue(env.getUblVCS().getBranches("").contains(rbUBLFixedVer.getName()));
		
		// check UBL versions
		assertEquals(env.getUblVer().toNextMinor(), dbUBL.getVersion());
		ReleaseBranch newUBLRB = new ReleaseBranch(compUBL, repos);
		assertEquals(env.getUblVer().toRelease(), newUBLRB.getCurrentVersion());
		
		// check unTillDb versions
		assertEquals(env.getUnTillDbVer(), dbUnTillDb.getVersion());
		
		// check unTill versions
		assertEquals(env.getUnTillVer().toNextMinor(), dbUnTill.getVersion());
		ReleaseBranch newUnTillRB = new ReleaseBranch(compUnTill, repos);
		assertEquals(env.getUnTillVer().toRelease(), newUnTillRB.getCurrentVersion());
		
		// check UBL mDeps
		List<Component> ublReleaseMDeps = rbUBLFixedVer.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		assertEquals(dbUnTillDb.getVersion().toRelease(), ublReleaseMDeps.get(0).getVersion());
		
		// check unTill mDeps
		List<Component> unTillReleaseMDeps = rbUnTillFixedVer.getMDeps();
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component unTillReleaseMDep : unTillReleaseMDeps) {
			if (unTillReleaseMDep.getName().equals(UBL)) {
				assertEquals(dbUBL.getVersion().toPreviousMinor().toRelease(), unTillReleaseMDep.getVersion());
			} else if (unTillReleaseMDep.getName().equals(UNTILLDB)) {
				assertEquals(dbUnTillDb.getVersion().toRelease(), unTillReleaseMDep.getVersion());
			} else {
				fail();
			}
		}
	}
	
	@Test
	public void testForkAll() throws Exception {
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
		
		assertTrue(env.getUnTillVCS().getBranches("").contains(rbUnTillFixedVer.getName()));
		assertTrue(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDbFixedVer.getName()));
		assertTrue(env.getUblVCS().getBranches("").contains(rbUBLFixedVer.getName()));
		
		// check UBL versions
		assertEquals(env.getUblVer().toNextMinor(), dbUBL.getVersion());
		ReleaseBranch newUBLRB = new ReleaseBranch(compUBL, repos);
		assertEquals(env.getUblVer().toRelease(), newUBLRB.getCurrentVersion());
		
		// check unTillDb versions
		assertEquals(env.getUnTillDbVer().toNextMinor(), dbUnTillDb.getVersion());
		ReleaseBranch newUnTillDbRB = new ReleaseBranch(compUnTillDb, repos);
		assertEquals(env.getUnTillDbVer().toRelease(), newUnTillDbRB.getCurrentVersion());
		
		// check unTill versions
		assertEquals(env.getUnTillVer().toNextMinor(), dbUnTill.getVersion());
		ReleaseBranch newUnTillRB = new ReleaseBranch(compUnTill, repos);
		assertEquals(env.getUnTillVer().toRelease(), newUnTillRB.getCurrentVersion());
		
		// check UBL mDeps
		List<Component> ublReleaseMDeps = rbUBLFixedVer.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toRelease(), ublReleaseMDeps.get(0).getVersion());
		
		// check unTill mDeps
		List<Component> unTillReleaseMDeps = rbUnTillFixedVer.getMDeps();
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component unTillReleaseMDep : unTillReleaseMDeps) {
			if (unTillReleaseMDep.getName().equals(UBL)) {
				assertEquals(dbUBL.getVersion().toPreviousMinor().toRelease(), unTillReleaseMDep.getVersion());
			} else if (unTillReleaseMDep.getName().equals(UNTILLDB)) {
				assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toRelease(), unTillReleaseMDep.getVersion());
			} else {
				fail();
			}
		}
	}
	
	@Test
	public void testActualizeMDeps() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		
		//fork all 
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(new NullProgress());
		
		// add feature to Release Branch. 
		
		
	}
}
