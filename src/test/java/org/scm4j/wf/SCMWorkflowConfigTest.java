package org.scm4j.wf;

import org.eclipse.jgit.api.errors.GitAPIException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.scm4j.actions.IAction;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.wf.conf.Credentials;
import org.scm4j.wf.conf.VCSRepository;
import org.scm4j.wf.conf.Version;

@PrepareForTest({VCSRepository.class, Credentials.class})
@RunWith(PowerMockRunner.class)
public class SCMWorkflowConfigTest {

	public static final String PRODUCT_UNTILL = "eu.untill:unTill";
	public static final String PRODUCT_UBL = "eu.untill:UBL";
	public static final String PRODUCT_UNTILLDB = "eu.untill:unTIllDb";
	
	private TestEnvironment env;

	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		PowerMockito.mockStatic(System.class);
		PowerMockito.when(System.getenv(Credentials.CREDENTIALS_LOCATION_ENV_VAR))
				.thenReturn("file://localhost/" + env.getCredsFile().getPath().replace("\\", "/"));
		PowerMockito.when(System.getenv(VCSRepository.REPOS_LOCATION_ENV_VAR))
				.thenReturn("file://localhost/" + env.getReposFile().getPath().replace("\\", "/"));
		
	}

	@After
	public void tearDown() {
		File testFolder = new File(TestEnvironment.TEST_ENVIRONMENT_DIR);
		if (testFolder.exists()) {
			testFolder.delete();
		}
	}
	
	@Test
	public void testUseLastVersions() {
		env.generateCommitWithVERTag(env.getUnTillVCS());
		env.generateCommitWithVERTag(env.getUnTillDbVCS());
		env.generateCommitWithVERTag(env.getUblVCS());
		
		SCMWorkflow wf = new SCMWorkflow(PRODUCT_UNTILL);
		
		IAction actionUnTill = wf.getProductionReleaseAction();
		checkUseLastReleaseAction(actionUnTill, null, PRODUCT_UNTILL, env.getUnTillVer());
		assertTrue(actionUnTill.getChildActions().size() == 2);
		
		IAction actionUBL = actionUnTill.getChildActions().get(0);
		checkUseLastReleaseAction(actionUBL, actionUnTill, PRODUCT_UBL, env.getUblVer());
		assertTrue(actionUBL.getChildActions().size() == 1);
		
		IAction actionUnTillDb = actionUnTill.getChildActions().get(1);
		checkUseLastReleaseAction(actionUnTillDb, actionUnTill, PRODUCT_UNTILLDB, env.getUnTillDbVer());
		
		IAction actionUBLUnTillDb = actionUBL.getChildActions().get(0);
		checkUseLastReleaseAction(actionUBLUnTillDb, actionUBL, PRODUCT_UNTILLDB, env.getUnTillDbVer());
	}
	
	@Test
	public void testProductionReleaseNewFeatures() {
		env.generateFeatureCommit(env.getUblVCS(), "feature commit");
		env.generateFeatureCommit(env.getUnTillVCS(), "feature commit");
		env.generateFeatureCommit(env.getUnTillDbVCS(), "feature commit");
		
		SCMWorkflow wf = new SCMWorkflow(PRODUCT_UNTILL);
		
		IAction actionUnTill = wf.getProductionReleaseAction();
		checkProductionReleaseAction(actionUnTill, null, ProductionReleaseReason.NEW_FEATURES, PRODUCT_UNTILL);
		assertTrue(actionUnTill.getChildActions().size() == 2);
		
		IAction actionUBL = actionUnTill.getChildActions().get(0);
		checkProductionReleaseAction(actionUBL, actionUnTill, ProductionReleaseReason.NEW_FEATURES, PRODUCT_UBL);
		assertTrue(actionUBL.getChildActions().size() == 1);
		
		IAction actionUnTillDb = actionUnTill.getChildActions().get(1);
		checkProductionReleaseAction(actionUnTillDb, actionUnTill, ProductionReleaseReason.NEW_FEATURES, PRODUCT_UNTILLDB);
		
		IAction actionUBLUnTillDb = actionUBL.getChildActions().get(0);
		checkProductionReleaseAction(actionUBLUnTillDb, actionUBL, ProductionReleaseReason.NEW_FEATURES, PRODUCT_UNTILLDB);
	}
	
	@Test
	public void testProductionReleaseHasNewFeaturedDependencies() {
		env.generateCommitWithVERTag(env.getUnTillVCS());
		env.generateCommitWithVERTag(env.getUblVCS());
		env.generateFeatureCommit(env.getUnTillDbVCS(), "feature commit");

		SCMWorkflow wf = new SCMWorkflow(PRODUCT_UNTILL);

		IAction actionUnTill = wf.getProductionReleaseAction();
		checkProductionReleaseAction(actionUnTill, null, ProductionReleaseReason.NEW_DEPENDENCIES, PRODUCT_UNTILL);
		assertTrue(actionUnTill.getChildActions().size() == 2);

		IAction actionUBL = actionUnTill.getChildActions().get(0);
		checkProductionReleaseAction(actionUBL, actionUnTill, ProductionReleaseReason.NEW_DEPENDENCIES, PRODUCT_UBL);
		assertTrue(actionUBL.getChildActions().size() == 1);

		IAction actionUnTillDb = actionUnTill.getChildActions().get(1);
		checkProductionReleaseAction(actionUnTillDb, actionUnTill, ProductionReleaseReason.NEW_FEATURES, PRODUCT_UNTILLDB);

		IAction actionUBLUnTillDb = actionUBL.getChildActions().get(0);
		checkProductionReleaseAction(actionUBLUnTillDb, actionUBL, ProductionReleaseReason.NEW_FEATURES, PRODUCT_UNTILLDB);
	}

	@Test
	public void testProductionReleaseHasNewerDependencyVersions() {
		env.generateCommitWithVERTag(env.getUnTillVCS());
		env.generateCommitWithVERTag(env.getUnTillDbVCS());
		env.generateCommitWithVERTag(env.getUblVCS());
		env.getUnTillVCS().setFileContent(null, SCMWorkflow.MDEPS_FILE_NAME,
				SCMWorkflowConfigTest.PRODUCT_UBL + ":" + env.getUblVer().toString() + "\r\n" +
				SCMWorkflowConfigTest.PRODUCT_UNTILLDB + ":1.0.0" + "\r\n",
				SCMActionProductionRelease.VCS_TAG_SCM_IGNORE + " old unTillDb version is used in mdeps file");

		SCMWorkflow wf = new SCMWorkflow(PRODUCT_UNTILL);

		IAction actionUnTill = wf.getProductionReleaseAction();
		checkProductionReleaseAction(actionUnTill, null, ProductionReleaseReason.NEW_DEPENDENCIES, PRODUCT_UNTILL);
		assertTrue(actionUnTill.getChildActions().size() == 2);

		IAction actionUBL = actionUnTill.getChildActions().get(0);
		checkUseLastReleaseAction(actionUBL, actionUnTill, PRODUCT_UBL, env.getUblVer());
		assertTrue(actionUBL.getChildActions().size() == 1);

		IAction actionUnTillDb = actionUnTill.getChildActions().get(1);
		checkUseLastReleaseAction(actionUnTillDb, actionUnTill, PRODUCT_UNTILLDB, env.getUnTillDbVer());

		IAction actionUBLUnTillDb = actionUBL.getChildActions().get(0);
		checkUseLastReleaseAction(actionUBLUnTillDb, actionUBL, PRODUCT_UNTILLDB, env.getUnTillDbVer());

	}
	
	private void checkAction(IAction action, IAction parentAction, String expectedName) {
		assertNotNull(action);
		assertEquals(action.getParent(), parentAction);
		assertEquals(action.getName(), expectedName);
		assertNotNull(action.getExecutionResults());
		assertTrue(action.getExecutionResults().isEmpty()); // because is not executed yet
	}
	
	private void checkUseLastReleaseAction(IAction action, IAction parentAction, String expectedName, Version expectedVersion) {
		checkAction(action, parentAction, expectedName);
		
		assertTrue(action instanceof SCMActionUseLastReleaseVersion);
		SCMActionUseLastReleaseVersion lv = (SCMActionUseLastReleaseVersion) action;
		assertEquals(lv.getVer(), expectedVersion);
	}
	
	private void checkProductionReleaseAction(IAction action, IAction parentAction, ProductionReleaseReason expectedReason, 
			String expectedName) {
		checkAction(action, parentAction, expectedName);
		
		assertTrue(action instanceof SCMActionProductionRelease);
		SCMActionProductionRelease pr = (SCMActionProductionRelease) action;
		assertEquals(pr.getReason(), expectedReason);
	}
	
	@Test
	public void testTagRelease() throws Exception {
		env.generateFeatureCommit(env.getUblVCS(), "feature commit");
		env.generateFeatureCommit(env.getUnTillVCS(), "feature commit");
		env.generateFeatureCommit(env.getUnTillDbVCS(), "feature commit");
		
		SCMWorkflow wf = new SCMWorkflow(PRODUCT_UNTILL);
		IAction actionReleaseUnTill = wf.getProductionReleaseAction();
		
		try (IProgress progress = new ProgressConsole("releasing " + actionReleaseUnTill.getName(), ">>> ", "<<< ")) {
			actionReleaseUnTill.execute(progress);
		}
		
		IAction actionTagUnTill = wf.getTagReleaseAction();
		
		try (IProgress progress = new ProgressConsole("tagging " + actionReleaseUnTill.getName(), ">>> ", "<<< ")) {
			actionTagUnTill.execute(progress);
		}
		
		List<VCSTag> tags = env.getUnTillVCS().getTags();
		assertNotNull(tags);
		assertTrue(tags.size() == 1);
		VCSTag tag = tags.get(0);
		assertEquals(tag.getTagName(), actionReleaseUnTill.getReleaseIntf().getNewVersion());
		assertEquals(tag.getTagMessage(), "tagMessage");
		assertEquals(tag.getRelatedCommit(), env.getUnTillVCS().getHeadCommit(actionReleaseUnTill.getReleaseIntf().getNewBranchName()));
	}

	private void checkChildActionsSupportsIRelease(List<IAction> childActions) {
		if (childActions == null) {
			return;
		}
		for (IAction action : childActions) {
			checkChildActionsSupportsIRelease(action.getChildActions());
			assertNotNull(action.getReleaseIntf());
		}
	}
}
