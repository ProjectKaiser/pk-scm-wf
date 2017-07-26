package org.scm4j.wf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branchstatus.DevelopBranch;
import org.scm4j.wf.branchstatus.ReleaseBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.scmactions.SCMActionForkReleaseBranch;
import org.scm4j.wf.scmactions.SCMActionUseLastReleaseVersion;

public class SCMWorkflowForkReleaseTest {
	
	private TestEnvironment env;
	private static final String UNTILL = TestEnvironment.PRODUCT_UNTILL;
	private static final String UNTILLDB = TestEnvironment.PRODUCT_UNTILLDB;
	private static final String UBL = TestEnvironment.PRODUCT_UBL;
	private VCSRepositories repos;
	private Component compUnTill;
	private Component compUnTillDb;
	private Component compUBL;
	//private DevelopBranch dbUnTill;
	private DevelopBranch dbUnTillDb;
	private DevelopBranch dbUBL;
	private ReleaseBranch rbUnTillFixedVer;
	private ReleaseBranch rbUnTillDbFixedVer;
	private ReleaseBranch rbUBLFixedVer;
	
	
	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		repos = VCSRepositories.loadVCSRepositories();
		compUnTill = new Component(UNTILL, repos);
		compUnTillDb = new Component(UNTILLDB, repos);
		compUBL = new Component(UBL, repos);
		//dbUnTill = new DevelopBranch(compUnTill);
		dbUnTillDb = new DevelopBranch(compUnTillDb);
		dbUBL = new DevelopBranch(compUBL);
		rbUnTillFixedVer = new ReleaseBranch(compUnTill, env.getUnTillVer(), repos);
		rbUnTillDbFixedVer = new ReleaseBranch(compUnTillDb, env.getUnTillDbVer(), repos);
		rbUBLFixedVer = new ReleaseBranch(compUBL, env.getUblVer(), repos);
	}

	@After
	public void tearDown() throws Exception {
		if (env != null) {
			env.close();
		}
	}
	
	@Test
	public void testForkReleaseForUnTill() throws Exception {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILL);
		Map<String, Class<?>> expected = new HashMap<>();
		expected.put(UNTILL, SCMActionForkReleaseBranch.class);
		expected.put(UBL, SCMActionUseLastReleaseVersion.class);
		expected.put(UNTILLDB, SCMActionUseLastReleaseVersion.class);
		checkChildActionsTypes(action, expected);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		assertTrue(env.getUnTillVCS().getBranches().contains(rbUnTillFixedVer.getReleaseBranchName()));
		assertFalse(env.getUnTillDbVCS().getBranches().contains(rbUnTillDbFixedVer.getReleaseBranchName()));
		assertFalse(env.getUblVCS().getBranches().contains(rbUBLFixedVer.getReleaseBranchName()));
		
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
	
	private void checkChildActionsTypes(IAction action, Map<String, Class<?>> expected) {
		for (IAction nestedAction : action.getChildActions()) {
			checkChildActionsTypes(nestedAction, expected);
		}
		
		if (!action.getClass().isAssignableFrom(expected.get(action.getName()))) {
			fail (action.toString() + " check failed. Expected: " + expected.get(action.getName()) + ", actual: " + action.getClass());
		}
		assertTrue(action.getClass().isAssignableFrom(expected.get(action.getName())));
	}

	@Test
	public void testForkReleaseForUnTillAndUBL() throws Exception {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILL);
		Map<String, Class<?>> expected = new HashMap<>();
		expected.put(UNTILL, SCMActionForkReleaseBranch.class);
		expected.put(UBL, SCMActionForkReleaseBranch.class);
		expected.put(UNTILLDB, SCMActionUseLastReleaseVersion.class);
		checkChildActionsTypes(action, expected);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		assertTrue(env.getUnTillVCS().getBranches().contains(rbUnTillFixedVer.getReleaseBranchName()));
		assertFalse(env.getUnTillDbVCS().getBranches().contains(rbUnTillDbFixedVer.getReleaseBranchName()));
		assertTrue(env.getUblVCS().getBranches().contains(rbUBLFixedVer.getReleaseBranchName()));
		
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
	public void testForkReleaseForUnTillAndUBLAndUnTillDb() throws Exception {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILL);
		Map<String, Class<?>> expected = new HashMap<>();
		expected.put(UNTILL, SCMActionForkReleaseBranch.class);
		expected.put(UBL, SCMActionForkReleaseBranch.class);
		expected.put(UNTILLDB, SCMActionForkReleaseBranch.class);
		checkChildActionsTypes(action, expected);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		assertTrue(env.getUnTillVCS().getBranches().contains(rbUnTillFixedVer.getReleaseBranchName()));
		assertTrue(env.getUnTillDbVCS().getBranches().contains(rbUnTillDbFixedVer.getReleaseBranchName()));
		assertTrue(env.getUblVCS().getBranches().contains(rbUBLFixedVer.getReleaseBranchName()));
		
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
		Map<String, Class<?>> expected = new HashMap<>();
		expected.put(UNTILLDB, SCMActionForkReleaseBranch.class);
		checkChildActionsTypes(action, expected);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}

		assertFalse(env.getUnTillVCS().getBranches().contains(rbUnTillFixedVer.getReleaseBranchName()));
		assertTrue(env.getUnTillDbVCS().getBranches().contains(rbUnTillDbFixedVer.getReleaseBranchName()));
		assertFalse(env.getUblVCS().getBranches().contains(rbUBLFixedVer.getReleaseBranchName()));
		
		wf = new SCMWorkflow();
		action = wf.getProductionReleaseAction(UNTILL);
		expected = new HashMap<>();
		expected.put(UNTILLDB, ActionNone.class);
		expected.put(UNTILL, SCMActionForkReleaseBranch.class);
		expected.put(UBL, SCMActionForkReleaseBranch.class);
		checkChildActionsTypes(action, expected);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		assertTrue(env.getUnTillVCS().getBranches().contains(rbUnTillFixedVer.getReleaseBranchName()));
		assertTrue(env.getUnTillDbVCS().getBranches().contains(rbUnTillDbFixedVer.getReleaseBranchName()));
		assertTrue(env.getUblVCS().getBranches().contains(rbUBLFixedVer.getReleaseBranchName()));
	}
}
