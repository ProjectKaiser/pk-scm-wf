package org.scm4j.wf;
import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.scmactions.ReleaseReason;
import org.scm4j.wf.scmactions.SCMActionBuild;
import org.scm4j.wf.scmactions.SCMActionFork;

import java.util.List;

import static org.junit.Assert.*;

public class WorkflowForkTest extends WorkflowTestBase {

	@Test
	public void testForkSingleComponent() throws Exception {
		SCMWorkflow wf = new SCMWorkflow();

		// check none if Develop Branch is IGNORED
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		assertTrue(action instanceof ActionNone);

		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		action = wf.getProductionReleaseAction(UNTILLDB);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		checkUnTillDbForked();
	}
	
	@Test
	public void testForkRootComponentWithMDeps() throws Exception {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILL);

		// fork unTill. Only this component should be forked since it only has new features
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}

		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill);
		// check branches
		assertTrue(env.getUnTillVCS().getBranches("").contains(rbUnTill.getName()));
		assertFalse(env.getUnTillDbVCS().getBranches("").contains(new ReleaseBranch(compUnTillDb).getName()));
		assertFalse(env.getUblVCS().getBranches("").contains(new ReleaseBranch(compUBL).getName()));

		// check versions
		Version verTrunk = dbUnTill.getVersion();
		ReleaseBranch newUnTillRB = new ReleaseBranch(compUnTill);
		Version verRelease = newUnTillRB.getCurrentVersion();
		assertEquals(env.getUnTillVer().toNextMinor(), verTrunk);
		assertEquals(env.getUnTillVer().toRelease(), verRelease);

		// check mDeps. Should contain versions from its current dev branches
		List<Component> unTillReleaseMDeps = rbUnTill.getMDeps();
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
		checkUnTillDbForked();
		
		// build unTillDb
		action = wf.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		checkUnTillDbBuilt();
		
		// fork UBL. It should be forked since it has new dependencies.
		// simulate BRANCHED dev branch status
		env.generateContent(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "test file", "test content", LogTag.SCM_VER);
		action = wf.getProductionReleaseAction(UBL);
		action.execute(new NullProgress());
		checkUBLForked();
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UBL, SCMActionFork.class);
		exp.put(UBL,  "reason", ReleaseReason.NEW_DEPENDENCIES);
		checkChildActionsTypes(action, exp);
		
		// build UBL
		action = wf.getProductionReleaseAction(UBL);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		checkUBLBuilt();
	}

	@Test
	public void testForkRootWithNotAllMDeps() throws Exception {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILL);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}

		ReleaseBranch rbUBL = new ReleaseBranch(compUBL);
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill);

		assertTrue(env.getUnTillVCS().getBranches("").contains(rbUnTill.getName()));
		assertFalse(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDb.getName()));
		assertTrue(env.getUblVCS().getBranches("").contains(rbUBL.getName()));
		
		// check UBL versions
		assertEquals(env.getUblVer().toNextMinor(), dbUBL.getVersion());
		assertEquals(env.getUblVer().toRelease(), rbUBL.getCurrentVersion());
		
		// check unTillDb versions
		assertEquals(env.getUnTillDbVer(), dbUnTillDb.getVersion());
		
		// check unTill versions
		assertEquals(env.getUnTillVer().toNextMinor(), dbUnTill.getVersion());
		assertEquals(env.getUnTillVer().toRelease(), rbUnTill.getCurrentVersion());
		
		// check UBL mDeps
		List<Component> ublReleaseMDeps = rbUBL.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		assertEquals(dbUnTillDb.getVersion().toRelease(), ublReleaseMDeps.get(0).getVersion());
		
		// check unTill mDeps
		List<Component> unTillReleaseMDeps = rbUnTill.getMDeps();
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
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		checkUnTillForked();
	}

	@Test
	public void testPatches() throws Exception {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();

		// fork all
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(new NullProgress());
		checkUnTillForked();

		// build all
		action = wf.getProductionReleaseAction(compUnTill);
		action.execute(new NullProgress());
		checkUnTillBuilt();

		// add feature to existing unTIllDb release. Next unTillDb patch should be released then
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), rbUnTillDb.getName(), "patch feature added");

		// Existing unTill and UBL release branches should actualize its mdeps first
		action = wf.getProductionReleaseAction(compUnTill);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UNTILL, SCMActionFork.class);
		exp.put(UNTILL, "reason", ReleaseReason.ACTUALIZE_MDEPS);
		exp.put(UBL, SCMActionFork.class);
		exp.put(UBL, "reason", ReleaseReason.ACTUALIZE_MDEPS);
		checkChildActionsTypes(action, exp);
		// actualize unTill and UBL mdeps
		try (IProgress progress = new ProgressConsole(action.getName(), ">>> ", "<<< ")) {
			action.execute(progress);
		}

		// now new unTillDb patch should be built
		action = wf.getProductionReleaseAction(compUnTill);
		exp = new Expectations();
		exp.put(UNTILL, SCMActionBuild.class);
		exp.put(UNTILL, "reason", ReleaseReason.NEW_DEPENDENCIES);
		exp.put(UBL, SCMActionBuild.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_DEPENDENCIES);
		exp.put(UNTILLDB, SCMActionBuild.class);
		exp.put(UNTILLDB, "reason", ReleaseReason.NEW_FEATURES);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.getName(), ">>> ", "<<< ")) {
			action.execute(progress);
		}

		// check unTill uses new untillDb and UBL patches in existing unTill release branch.
		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill);
		List<Component> mdeps = rbUnTill.getMDeps();
		for (Component mdep : mdeps) {
			if (mdep.getName().equals(UBL)) {
				assertEquals(dbUBL.getVersion().toPreviousMinor().toNextPatch().toRelease(), mdep.getVersion());
			} else if (mdep.getName().equals(UNTILLDB)) {
				assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toNextPatch().toRelease(), mdep.getVersion());
			} else {
				fail();
			}
		}
	}
}
