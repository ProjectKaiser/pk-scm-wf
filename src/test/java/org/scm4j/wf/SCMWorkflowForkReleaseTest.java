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
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branchstatus.DevelopBranch;
import org.scm4j.wf.branchstatus.ReleaseBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.scmactions.SCMActionForkReleaseBranch;
import org.scm4j.wf.scmactions.SCMActionBuild;
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
	private DevelopBranch dbUnTill;
	private DevelopBranch dbUnTillDb;
	private DevelopBranch dbUBL;
	private ReleaseBranch rbUnTill;
	private ReleaseBranch rbUnTillDb;
	private ReleaseBranch rbUBL;
	
	
	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		repos = VCSRepositories.loadVCSRepositories();
		compUnTill = new Component(UNTILL, repos);
		compUnTillDb = new Component(UNTILLDB, repos);
		compUBL = new Component(UBL, repos);
		dbUnTill = new DevelopBranch(compUnTill);
		dbUnTillDb = new DevelopBranch(compUnTillDb);
		dbUBL = new DevelopBranch(compUBL);
		rbUnTill = new ReleaseBranch(compUnTill, env.getUnTillVer(), repos);
		rbUnTillDb = new ReleaseBranch(compUnTillDb, env.getUnTillDbVer(), repos);
		rbUBL = new ReleaseBranch(compUBL, env.getUblVer(), repos);
	}

	@After
	public void tearDown() throws Exception {
		if (env != null) {
			env.close();
		}
	}
	
	@Test
	public void testForkReleaseForUnTill() {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILL);
		Map<String, Class<?>> expected = new HashMap<>();
		expected.put(UNTILL, SCMActionForkReleaseBranch.class);
		expected.put(UBL, SCMActionUseLastReleaseVersion.class);
		expected.put(UNTILLDB, SCMActionUseLastReleaseVersion.class);
		checkChildActionsTypes(action, expected);
		
		action.execute(new ProgressConsole(UNTILL, ">>> ", "<<< "));
		
		assertTrue(env.getUnTillVCS().getBranches().contains(dbUnTill.getReleaseBranchName()));
		assertFalse(env.getUnTillDbVCS().getBranches().contains(dbUnTillDb.getReleaseBranchName()));
		assertFalse(env.getUblVCS().getBranches().contains(dbUBL.getReleaseBranchName()));
		
		List<Component> unTillReleaseMDeps = rbUnTill.getMDeps();
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

		assertTrue(action.getClass().isAssignableFrom(expected.get(action.getName())));
	}

	@Test
	public void testForkReleaseForUnTillAndUBL() {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILL);
		Map<String, Class<?>> expected = new HashMap<>();
		expected.put(UNTILL, SCMActionForkReleaseBranch.class);
		expected.put(UBL, SCMActionForkReleaseBranch.class);
		expected.put(UNTILLDB, SCMActionUseLastReleaseVersion.class);
		checkChildActionsTypes(action, expected);
		
		action.execute(new ProgressConsole(UNTILL, ">>> ", "<<< "));
		
		
		assertTrue(env.getUnTillVCS().getBranches().contains(dbUnTill.getReleaseBranchName()));
		assertFalse(env.getUnTillDbVCS().getBranches().contains(dbUnTillDb.getReleaseBranchName()));
		assertTrue(env.getUblVCS().getBranches().contains(dbUBL.getReleaseBranchName()));
		
		List<Component> unTillReleaseMDeps = rbUnTill.getMDeps();
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component unTillReleaseMDep : unTillReleaseMDeps) {
			if (unTillReleaseMDep.getName().equals(UBL)) {
				assertEquals(dbUBL.getVersion().toReleaseString(), unTillReleaseMDep.getVersion().toReleaseString());
			} else if (unTillReleaseMDep.getName().equals(UNTILLDB)) {
				assertEquals(dbUnTillDb.getVersion().toPreviousMinorRelease(), unTillReleaseMDep.getVersion().toReleaseString());
			} else {
				fail();
			}
		}
		
		List<Component> ublReleaseMDeps = rbUBL.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(dbUnTillDb.getVersion().toPreviousMinorRelease(), ublReleaseMDeps.get(0).getVersion().toReleaseString());
	}
	
	@Test
	public void testForkReleaseForUnTillAndUBLAndUnTillDb() {
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
		
		action.execute(new ProgressConsole(UNTILL, ">>> ", "<<< "));
		
		assertTrue(env.getUnTillVCS().getBranches().contains(dbUnTill.getReleaseBranchName()));
		assertTrue(env.getUnTillDbVCS().getBranches().contains(dbUnTillDb.getReleaseBranchName()));
		assertTrue(env.getUblVCS().getBranches().contains(dbUBL.getReleaseBranchName()));
		
		List<Component> unTillReleaseMDeps = rbUnTill.getMDeps();
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component unTillReleaseMDep : unTillReleaseMDeps) {
			if (unTillReleaseMDep.getName().equals(UBL)) {
				assertEquals(dbUBL.getVersion().toReleaseString(), unTillReleaseMDep.getVersion().toReleaseString());
			} else if (unTillReleaseMDep.getName().equals(UNTILLDB)) {
				assertEquals(dbUnTillDb.getVersion().toReleaseString(), unTillReleaseMDep.getVersion().toReleaseString());
			} else {
				fail();
			}
		}
		
		List<Component> ublReleaseMDeps = rbUBL.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(dbUnTillDb.getVersion().toReleaseString(), ublReleaseMDeps.get(0).getVersion().toReleaseString());
		
		assertTrue(rbUnTillDb.getMDeps().isEmpty());
	}
	
	@Test
	public void testSkipBuildsIfParentUnforked() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		Map<String, Class<?>> expected = new HashMap<>();
		expected.put(UNTILLDB, SCMActionForkReleaseBranch.class);
		checkChildActionsTypes(action, expected);
		
		action.execute(new ProgressConsole(UNTILLDB, ">>> ", "<<< "));

		assertFalse(env.getUnTillVCS().getBranches().contains(rbUnTill.getReleaseBranchName()));
		assertTrue(env.getUnTillDbVCS().getBranches().contains(rbUnTillDb.getReleaseBranchName()));
		assertFalse(env.getUblVCS().getBranches().contains(rbUBL.getReleaseBranchName()));
		
		wf = new SCMWorkflow();
		action = wf.getProductionReleaseAction(UNTILL);
		expected = new HashMap<>();
		expected.put(UNTILLDB, ActionNone.class);
		expected.put(UNTILL, SCMActionForkReleaseBranch.class);
		expected.put(UBL, SCMActionForkReleaseBranch.class);
		checkChildActionsTypes(action, expected);
		
		action.execute(new ProgressConsole(UNTILL, ">>> ", "<<< "));
		
		assertTrue(env.getUnTillVCS().getBranches().contains(rbUnTill.getReleaseBranchName()));
		assertTrue(env.getUnTillDbVCS().getBranches().contains(rbUnTillDb.getReleaseBranchName()));
		assertTrue(env.getUblVCS().getBranches().contains(rbUBL.getReleaseBranchName()));
	}
}
