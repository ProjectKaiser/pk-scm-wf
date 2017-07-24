package org.scm4j.wf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.wf.actions.ActionError;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branchstatus.DevelopBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.exceptions.EComponentConfig;
import org.scm4j.wf.scmactions.ProductionReleaseReason;
import org.scm4j.wf.scmactions.SCMActionForkReleaseBranch;
import org.scm4j.wf.scmactions.SCMActionBuild;
import org.scm4j.wf.scmactions.SCMActionUseLastReleaseVersion;

public class SCMWorkflowGetActionTest {
	
	private TestEnvironment env;
	private VCSRepositories repos;
	private Component compUnTill;
	private Component compUBL;
	
	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		repos = VCSRepositories.loadVCSRepositories();
		compUnTill = new Component(TestEnvironment.PRODUCT_UNTILL, repos);
		compUBL = new Component(TestEnvironment.PRODUCT_UBL, repos);
	}

	@After
	public void tearDown() throws Exception {
		if (env != null) {
			env.close();
		}
	}
	
	@Test
	public void testErrorIfNoVersionFile() {
		env.getUnTillVCS().removeFile(null, SCMWorkflow.VER_FILE_NAME, "ver file removed");
		SCMWorkflow wf = new SCMWorkflow(TestEnvironment.PRODUCT_UNTILL);
		try {
			wf.getProductionReleaseActionRoot(null);
			fail();
		} catch (EComponentConfig e) {
		}
	}
	
	@Test
	public void testActionNoneIfHasErrorActions() {
		SCMWorkflow wf = new SCMWorkflow(compUnTill, repos);
		List<IAction> childActions = Arrays.<IAction>asList(new ActionError(compUnTill, new ArrayList<IAction>(), "test error action cause"));
		IAction action = wf.getProductionReleaseActionRoot(childActions);
		assertTrue(action instanceof ActionNone);
		ActionNone actionNone = (ActionNone) action;
		assertEquals(compUnTill, actionNone.getComponent());
		assertEquals(childActions, actionNone.getChildActions());
	}
	
	@Test
	public void testProductionReleaseIfNewFeatures() {
		DevelopBranch db = new DevelopBranch(compUnTill);
		env.getUnTillVCS().createBranch(db.getName(), db.getReleaseBranchName(), "release branch created");
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow(compUnTill, repos);
		List<IAction> childActions = new ArrayList<IAction>();
		IAction action = wf.getProductionReleaseActionRoot(childActions);
		assertTrue(action instanceof SCMActionBuild);
		SCMActionBuild actionRelease = (SCMActionBuild) action;
		assertEquals(actionRelease.getReason(), ProductionReleaseReason.NEW_FEATURES);
		assertEquals(compUnTill, actionRelease.getComponent());
		assertEquals(childActions, actionRelease.getChildActions());
	}
	
	@Test
	public void testProductionReleaseNewDependencies() {
		DevelopBranch db = new DevelopBranch(compUnTill);
		env.getUnTillVCS().createBranch(db.getName(), db.getReleaseBranchName(), "release branch created");
		SCMWorkflow wf = new SCMWorkflow(compUnTill, repos);
		List<IAction> childActions = Arrays.<IAction>asList(new SCMActionBuild(compUBL, new ArrayList<IAction>(), ProductionReleaseReason.NEW_FEATURES));
		IAction action = wf.getProductionReleaseActionRoot(childActions);
		assertTrue(action instanceof SCMActionBuild);
		SCMActionBuild actionRelease = (SCMActionBuild) action;
		assertEquals(actionRelease.getReason(), ProductionReleaseReason.NEW_DEPENDENCIES);
		assertEquals(compUnTill, actionRelease.getComponent());
		assertEquals(childActions, actionRelease.getChildActions());
	}
	
	@Test
	public void testProductionReleaseNewDependenciesIfSignificantActions() {
		DevelopBranch db = new DevelopBranch(compUnTill);
		env.getUnTillVCS().createBranch(db.getName(), db.getReleaseBranchName(), "release branch created");
		SCMWorkflow wf = new SCMWorkflow(compUnTill, repos);
		List<IAction> childActions = Arrays.<IAction>asList(new SCMActionBuild(compUBL, new ArrayList<IAction>(), ProductionReleaseReason.NEW_FEATURES));
		IAction action = wf.getProductionReleaseActionRoot(childActions);
		assertTrue(action instanceof SCMActionBuild);
		SCMActionBuild actionRelease = (SCMActionBuild) action;
		assertEquals(actionRelease.getReason(), ProductionReleaseReason.NEW_DEPENDENCIES);
		assertEquals(compUnTill, actionRelease.getComponent());
		assertEquals(childActions, actionRelease.getChildActions());
	}

	@Test
	public void testUseLastRelease() {
		DevelopBranch db = new DevelopBranch(compUnTill);
		env.getUnTillVCS().createBranch(db.getName(), db.getReleaseBranchName(), "release branch created");
		SCMWorkflow wf = new SCMWorkflow(compUnTill, repos);
		List<IAction> childActions = new ArrayList<IAction>();
		IAction action = wf.getProductionReleaseActionRoot(childActions);
		assertTrue(action instanceof SCMActionUseLastReleaseVersion);
		SCMActionUseLastReleaseVersion lastRelease = (SCMActionUseLastReleaseVersion) action;
		assertEquals(compUnTill, lastRelease.getComponent());
		assertEquals(childActions, lastRelease.getChildActions());
		assertTrue(lastRelease.getVersion().equals(env.getUnTillVer()));
		
		env.generateLogTag(env.getUnTillVCS(), db.getName(), LogTag.SCM_IGNORE);
		action = wf.getProductionReleaseActionRoot(childActions);
		assertTrue(action instanceof SCMActionUseLastReleaseVersion);
		lastRelease = (SCMActionUseLastReleaseVersion) action;
		assertEquals(compUnTill, lastRelease.getComponent());
		assertEquals(childActions, lastRelease.getChildActions());
		assertTrue(lastRelease.getVersion().equals(env.getUnTillVer()));
	}
	
	@Test
	public void testForkBranch() {
		SCMWorkflow wf = new SCMWorkflow(compUnTill, repos);
		List<IAction> childActions = Arrays.<IAction>asList(new SCMActionBuild(compUBL, new ArrayList<IAction>(), ProductionReleaseReason.NEW_FEATURES));
		IAction action = wf.getProductionReleaseActionRoot(childActions);
		assertTrue(action instanceof SCMActionForkReleaseBranch);
		SCMActionForkReleaseBranch forkRelease = (SCMActionForkReleaseBranch) action;
		assertEquals(compUnTill, forkRelease.getComponent());
		assertEquals(childActions, forkRelease.getChildActions());
		
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		childActions = new ArrayList<IAction>();
		action = wf.getProductionReleaseActionRoot(childActions);
		assertTrue(action instanceof SCMActionForkReleaseBranch);
		forkRelease = (SCMActionForkReleaseBranch) action;
		assertEquals(compUnTill, forkRelease.getComponent());
		assertEquals(childActions, forkRelease.getChildActions());
	}
}
