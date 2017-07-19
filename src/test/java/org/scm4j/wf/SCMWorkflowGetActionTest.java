package org.scm4j.wf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.actions.ActionError;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.conf.*;
import org.scm4j.wf.exceptions.EConfig;
import org.scm4j.wf.exceptions.EComponentConfig;
import org.scm4j.wf.scmactions.ProductionReleaseReason;
import org.scm4j.wf.scmactions.SCMActionProductionRelease;
import org.scm4j.wf.scmactions.SCMActionUseLastReleaseVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class SCMWorkflowGetActionTest {
	
	private TestEnvironment env;
	private VCSRepositories repos;
	
	
	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		repos = VCSRepositories.loadVCSRepositories();
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
		Component compUnTill = new Component(TestEnvironment.PRODUCT_UNTILL, repos);
		SCMWorkflow wf = new SCMWorkflow(compUnTill, repos);
		List<IAction> childActions = Arrays.<IAction>asList(new ActionError(compUnTill, new ArrayList<>(), "test error action cause"));
		IAction action = wf.getProductionReleaseActionRoot(childActions);
		assertTrue(action instanceof ActionNone);
		ActionNone actionNone = (ActionNone) action;
		assertEquals(compUnTill, actionNone.getComponent());
		assertEquals(childActions, actionNone.getChildActions());
	}
	
	@Test
	public void testProductionReleaseNewFeatures() {
		Mockito.doReturn(COMMIT_FEATURE).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		IAction action = wf.getProductionReleaseActionRoot(new ArrayList<IAction>());
		assertTrue(action instanceof SCMActionProductionRelease);
		SCMActionProductionRelease r = (SCMActionProductionRelease) action;
		assertEquals(r.getReason(), ProductionReleaseReason.NEW_FEATURES);
	}
	
	@Test 
	public void testActionNoneIfNoVersionFile() {
		Mockito.doReturn(false).when(mockedVcs).fileExists(testRepo.getDevBranch(), SCMWorkflow.VER_FILE_NAME);
		Mockito.doReturn(COMMIT_FEATURE).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		try {
			wf.getProductionReleaseActionRoot(new ArrayList<IAction>());
			fail();
		} catch (EComponentConfig e) {
			
		}
	}
	
	@Test
	public void testActionNoneIfHasErrors() {
		List<Component> testMDeps = Collections.singletonList(new Component(TEST_DEP + ":1.0.0", mockedRepos));
		wf.setMDeps(testMDeps);
		List<IAction> childActions = new ArrayList<>();
		childActions.add(new ActionError(new Component(TEST_DEP + ":1.0.0", mockedRepos), Collections.<IAction>emptyList(), "test error cause"));
		
		IAction res = wf.getProductionReleaseActionRoot(childActions);
		assertTrue(res instanceof ActionNone);
	}
	
	@Test
	public void testProductionReleaseNewDependencies() {
		Mockito.doReturn("0.0.0").when(mockedVcs).getFileContent(TEST_MASTER_BRANCH, SCMWorkflow.VER_FILE_NAME);
		Mockito.doReturn(COMMIT_VER).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		List<Component> testMDeps = Collections.singletonList(new Component(TEST_DEP + ":1.0.0", mockedRepos));
		wf.setMDeps(testMDeps);
		List<IAction> childActions = new ArrayList<>();
		childActions.add(new SCMActionUseLastReleaseVersion(new Component(TEST_DEP + ":1.0.0", mockedRepos), Collections.<IAction>emptyList()));
		IAction action = wf.getProductionReleaseActionRoot(childActions);
		assertTrue(action instanceof SCMActionProductionRelease);
		SCMActionProductionRelease pr = (SCMActionProductionRelease) action;
		assertEquals(pr.getReason(), ProductionReleaseReason.NEW_DEPENDENCIES);
	}

	@Test
	public void testUseLastReleaseIfNoFeatures() {
		Mockito.doReturn(COMMIT_VER).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		Version ver = new Version("1.0.0");
		Mockito.doReturn("1.0.0").when(mockedVcs).getFileContent(TEST_MASTER_BRANCH, SCMWorkflow.VER_FILE_NAME);
		IAction action = wf.getProductionReleaseActionRoot(new ArrayList<IAction>());
		assertTrue(action instanceof SCMActionUseLastReleaseVersion);
		SCMActionUseLastReleaseVersion lastRelease = (SCMActionUseLastReleaseVersion) action;
		assertTrue(lastRelease.getVersion().equals(ver));
	}

	@Test
	public void testUseLastReleaseIfIgnore() {
		Mockito.doReturn(COMMIT_IGNORE).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		Version ver = new Version("1.0.0");
		Mockito.doReturn("1.0.0").when(mockedVcs).getFileContent(TEST_MASTER_BRANCH, SCMWorkflow.VER_FILE_NAME);
		IAction action = wf.getProductionReleaseActionRoot(new ArrayList<IAction>());
		assertTrue(action instanceof SCMActionUseLastReleaseVersion);
		SCMActionUseLastReleaseVersion lastRelease = (SCMActionUseLastReleaseVersion) action;
		assertTrue(lastRelease.getVersion().equals(ver));
	}
}
