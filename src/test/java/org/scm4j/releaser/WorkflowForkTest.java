package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.CurrentReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.scmactions.ReleaseReason;
import org.scm4j.releaser.scmactions.SCMActionBuild;
import org.scm4j.releaser.scmactions.SCMActionFork;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class WorkflowForkTest extends WorkflowTestBase {

	@Test
	public void testForkSingleComponent() throws Exception {
		SCMReleaser releaser = new SCMReleaser();

		// check none if Develop Branch is IGNORED
		IAction action = releaser.getActionTree(UNTILLDB);
		assertTrue(action instanceof ActionNone);

		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		action = releaser.getActionTree(UNTILLDB);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		checkUnTillDbForked();
	}
	
	@Test
	public void testForkRootComponentWithMDeps() throws Exception {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(UNTILL);

		// fork unTill. Only this component should be forked since it only has new features
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}

		CurrentReleaseBranch crbUnTill = new CurrentReleaseBranch(compUnTill);
		// check branches
		assertTrue(env.getUnTillVCS().getBranches("").contains(crbUnTill.getName()));
		assertFalse(env.getUnTillDbVCS().getBranches("").contains(new CurrentReleaseBranch(compUnTillDb).getName()));
		assertFalse(env.getUblVCS().getBranches("").contains(new CurrentReleaseBranch(compUBL).getName()));

		// check versions
		Version verTrunk = dbUnTill.getVersion();
		CurrentReleaseBranch newUnTillCRB = new CurrentReleaseBranch(compUnTill);
		Version verRelease = newUnTillCRB.getHeadVersion();
		assertEquals(env.getUnTillVer().toNextMinor(), verTrunk);
		assertEquals(env.getUnTillVer().toRelease(), verRelease);

		// check mDeps. Should contain versions from its current dev branches
		List<Component> unTillReleaseMDeps = crbUnTill.getMDeps();
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
		SCMReleaser releaser = new SCMReleaser();
		
		// fork unTillDb
		IAction action = releaser.getActionTree(UNTILLDB);
		action.execute(new NullProgress());
		checkUnTillDbForked();
		
		// build unTillDb
		action = releaser.getActionTree(UNTILLDB);
		action.execute(new NullProgress());
		checkUnTillDbBuilt();
		
		// fork UBL. It should be forked since it has new dependencies.
		// simulate BRANCHED dev branch status
		env.generateContent(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "test file", "test content", LogTag.SCM_VER);
		action = releaser.getActionTree(UBL);
		action.execute(new NullProgress());
		checkUBLForked();
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UBL, SCMActionFork.class);
		checkChildActionsTypes(action, exp);
		
		// build UBL
		action = releaser.getActionTree(UBL);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		checkUBLBuilt();
	}

	@Test
	public void testForkRootWithNotAllMDeps() throws Exception {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(UNTILL);
		Expectations exp = new Expectations();
		exp.put(UNTILL, SCMActionFork.class);
		exp.put(UBL, SCMActionFork.class);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}

		CurrentReleaseBranch crbUBL = new CurrentReleaseBranch(compUBL);
		CurrentReleaseBranch crbUnTillDb = new CurrentReleaseBranch(compUnTillDb);
		CurrentReleaseBranch crbUnTill = new CurrentReleaseBranch(compUnTill);

		assertTrue(env.getUnTillVCS().getBranches("").contains(crbUnTill.getName()));
		assertFalse(env.getUnTillDbVCS().getBranches("").contains(crbUnTillDb.getName()));
		assertTrue(env.getUblVCS().getBranches("").contains(crbUBL.getName()));
		
		// check UBL versions
		assertEquals(env.getUblVer().toNextMinor(), dbUBL.getVersion());
		assertEquals(env.getUblVer().toRelease(), crbUBL.getHeadVersion());
		
		// check unTillDb versions
		assertEquals(env.getUnTillDbVer(), dbUnTillDb.getVersion());
		
		// check unTill versions
		assertEquals(env.getUnTillVer().toNextMinor(), dbUnTill.getVersion());
		assertEquals(env.getUnTillVer().toRelease(), crbUnTill.getHeadVersion());
		
		// check UBL mDeps
		List<Component> ublReleaseMDeps = crbUBL.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		assertEquals(dbUnTillDb.getVersion().toRelease(), ublReleaseMDeps.get(0).getVersion());
		
		// check unTill mDeps
		List<Component> unTillReleaseMDeps = crbUnTill.getMDeps();
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
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(UNTILL);
		Expectations exp = new Expectations();
		exp.put(UNTILL, SCMActionFork.class);
		exp.put(UBL, SCMActionFork.class);
		exp.put(UNTILLDB, SCMActionFork.class);
		checkChildActionsTypes(action, exp);
		
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
		SCMReleaser releaser = new SCMReleaser();

		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(new NullProgress());
		checkUnTillForked();

		// build all
		action = releaser.getActionTree(compUnTill);
		action.execute(new NullProgress());
		checkUnTillBuilt();

		// add feature to existing unTIllDb release. Next unTillDb patch should be released then
		CurrentReleaseBranch crbUnTillDb = new CurrentReleaseBranch(compUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), crbUnTillDb.getName(), "patch feature added");

		// Existing unTill and UBL release branches should actualize its mdeps first
		action = releaser.getActionTree(compUnTill);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UNTILL, SCMActionFork.class);
		exp.put(UBL, SCMActionFork.class);
		checkChildActionsTypes(action, exp);
		// actualize unTill and UBL mdeps
		try (IProgress progress = new ProgressConsole(action.getName(), ">>> ", "<<< ")) {
			action.execute(progress);
		}

		// check unTill uses new untillDb and UBL versions in existing unTill release branch.
		CurrentReleaseBranch crbUnTill = new CurrentReleaseBranch(compUnTill);
		List<Component> mdeps = crbUnTill.getMDeps();
		for (Component mdep : mdeps) {
			if (mdep.getName().equals(UBL)) {
				assertEquals(dbUBL.getVersion().toPreviousMinor().toNextPatch().toRelease(), mdep.getVersion());
			} else if (mdep.getName().equals(UNTILLDB)) {
				assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toNextPatch().toRelease(), mdep.getVersion());
			} else {
				fail();
			}
		}

		// now new unTillDb patch should be built
		action = releaser.getActionTree(compUnTill);
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
		crbUnTill = new CurrentReleaseBranch(compUnTill);
		mdeps = crbUnTill.getMDeps();
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
	
	@Test
	public void testUseExactDevVersionsAsIsInRelease() {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		Component compUnTillVersioned = new Component(UNTILL + ":" + env.getUnTillVer().toRelease());
		Component compUnTillDbVersioned = new Component(UNTILLDB + ":" + env.getUnTillDbVer().toRelease());
		Component compUblVersioned = new Component(UBL + ":" + env.getUblVer().toRelease());
		MDepsFile mDepsFile = new MDepsFile(Arrays.asList(compUnTillDbVersioned, compUblVersioned));
		env.getUnTillVCS().setFileContent(compUnTillVersioned.getVcsRepository().getDevBranch(), SCMReleaser.MDEPS_FILE_NAME, mDepsFile.toFileContent(), "-SNAPSHOT added to mDeps versions");
		SCMReleaser releaser = new SCMReleaser();

		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(new NullProgress());
		
		// check mDeps versions. All of them should be kept unchanged
		// UBL mDeps
		CurrentReleaseBranch crbUBL = new CurrentReleaseBranch(compUBL);
		List<Component> ublReleaseMDeps = crbUBL.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		assertEquals(env.getUnTillDbVer().toRelease(), ublReleaseMDeps.get(0).getVersion());
				
		
		// unTill mDeps
		CurrentReleaseBranch crbUnTill = new CurrentReleaseBranch(compUnTill);
		List<Component> unTillReleaseMDeps = crbUnTill.getMDeps();
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component mDep : unTillReleaseMDeps) {
			if (mDep.getName().equals(UBL)) {
				assertEquals(env.getUblVer().toRelease(), mDep.getVersion());
			} else if (mDep.getName().equals(UNTILLDB)) {
				assertEquals(env.getUnTillDbVer().toRelease(), mDep.getVersion());
			} else {
				fail();
			}
		}
	}
	
	@Test
	public void testActualizeExactSnapshotDevVersionsInRelease() {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		Component compUnTillVersioned = new Component(UNTILL + ":" + env.getUnTillVer().toSnapshot());
		Component compUnTillDbVersioned = new Component(UNTILLDB + ":" + env.getUnTillDbVer().toSnapshot());
		Component compUblVersioned = new Component(UBL + ":" + env.getUblVer().toSnapshot());
		MDepsFile mDepsFile = new MDepsFile(Arrays.asList(compUnTillDbVersioned, compUblVersioned));
		env.getUnTillVCS().setFileContent(compUnTillVersioned.getVcsRepository().getDevBranch(), SCMReleaser.MDEPS_FILE_NAME, mDepsFile.toFileContent(), "-SNAPSHOT added to mDeps versions");
		SCMReleaser releaser = new SCMReleaser();

		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(new NullProgress());
		
		// check mDeps versions. All of them should be replaced with new ones with no snapshot
		// UBL mDeps
		CurrentReleaseBranch crbUBL = new CurrentReleaseBranch(compUBL);
		List<Component> ublReleaseMDeps = crbUBL.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toRelease(), ublReleaseMDeps.get(0).getVersion());
				
		
		// unTill mDeps
		CurrentReleaseBranch crbUnTill = new CurrentReleaseBranch(compUnTill);
		List<Component> unTillReleaseMDeps = crbUnTill.getMDeps();
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component mDep : unTillReleaseMDeps) {
			if (mDep.getName().equals(UBL)) {
				assertEquals(dbUBL.getVersion().toPreviousMinor().toRelease(), mDep.getVersion());
			} else if (mDep.getName().equals(UNTILLDB)) {
				assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toRelease(), mDep.getVersion());
			} else {
				fail();
			}
		}
	}
}
