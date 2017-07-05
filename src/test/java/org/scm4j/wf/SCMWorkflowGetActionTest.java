package org.scm4j.wf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.scm4j.actions.ActionError;
import org.scm4j.actions.ActionNone;
import org.scm4j.actions.IAction;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.model.Dep;
import org.scm4j.wf.model.VCSRepository;
import org.scm4j.wf.model.VCSType;

@PrepareForTest(VCSFactory.class)
@RunWith(PowerMockRunner.class)
public class SCMWorkflowGetActionTest {

	private static final String TEST_DEP = "test:dep";
	private static final String TEST_MASTER_BRANCH = "test master branch";
	private static final VCSCommit COMMIT_FEATURE = new VCSCommit("test revision", "feature commit", null);
	private static final VCSCommit COMMIT_VER = new VCSCommit("test revision", SCMActionProductionRelease.VCS_TAG_SCM_VER, null);
	private static final VCSCommit COMMIT_IGNORE = new VCSCommit("test revision", SCMActionProductionRelease.VCS_TAG_SCM_IGNORE, null);
	
	@Mock
	IVCS mockedVcs;

	@Mock
	IVCSWorkspace ws;
	
	private Map<String, VCSRepository> vcsRepos;
	private VCSRepository testRepo;
	private SCMWorkflow wf;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		testRepo = new VCSRepository();
		vcsRepos = new HashMap<>();
		testRepo.setDevBranch(TEST_MASTER_BRANCH);
		testRepo.setName(TEST_DEP);
		testRepo.setType(VCSType.GIT);
		vcsRepos.put(testRepo.getName(), testRepo);
		PowerMockito.mockStatic(VCSFactory.class);
		PowerMockito.when(VCSFactory.getIVCS(testRepo, ws)).thenReturn(mockedVcs);
		wf = new SCMWorkflow(TEST_DEP, vcsRepos, ws);
		Mockito.doReturn(true).when(mockedVcs).fileExists(testRepo.getDevBranch(), SCMWorkflow.VER_FILE_NAME);
	}
	
	@Test
	public void testProductionReleaseNewFeatures() {
		Mockito.doReturn(COMMIT_FEATURE).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		IAction action = wf.getProductionReleaseAction();
		assertTrue(action instanceof SCMActionProductionRelease);
		SCMActionProductionRelease r = (SCMActionProductionRelease) action;
		assertEquals(r.getReason(), ProductionReleaseReason.NEW_FEATURES);
	}
	
	@Test 
	public void testActionError() {
		Mockito.doReturn(false).when(mockedVcs).fileExists(testRepo.getDevBranch(), SCMWorkflow.VER_FILE_NAME);
		Mockito.doReturn(COMMIT_FEATURE).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		IAction action = wf.getProductionReleaseAction();
		assertTrue(action instanceof ActionError);
		assertNotNull(((ActionError) action).getCause());
	}
	
	@Test
	public void testActionNone() {
		List<Dep> testMDeps = Collections.singletonList(new Dep(TEST_DEP + ":1.0.0", vcsRepos));
		wf.setMDeps(testMDeps);
		wf.setChildActions(Collections.singletonList((IAction) new ActionError(testRepo, Collections.<IAction>emptyList(), TEST_MASTER_BRANCH, "test cause", ws)));
		IAction res = wf.getProductionReleaseAction();
		assertTrue(res instanceof ActionNone);
	}
	
	@Test
	public void testProductionReleaseNewDependencies() {
		Mockito.doReturn("0.0.0").when(mockedVcs).getFileContent(TEST_MASTER_BRANCH, SCMWorkflow.VER_FILE_NAME);
		Mockito.doReturn(COMMIT_VER).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		List<Dep> testMDeps = Collections.singletonList(new Dep(TEST_DEP + ":1.0.0", vcsRepos));
		wf.setMDeps(testMDeps);
		wf.setChildActions(Collections.<IAction>singletonList(new SCMActionUseLastReleaseVersion(testRepo, Collections.<IAction>emptyList(), TEST_MASTER_BRANCH, ws)));
		IAction action = wf.getProductionReleaseAction();
		assertTrue(action instanceof SCMActionProductionRelease);
		SCMActionProductionRelease pr = (SCMActionProductionRelease) action;
		assertEquals(pr.getReason(), ProductionReleaseReason.NEW_DEPENDENCIES);
	}

	@Test
	public void testUseLastReleaseIfNoFeatures() {
		Mockito.doReturn(COMMIT_VER).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		Version ver = new Version("1.0.0");
		Mockito.doReturn("1.0.0").when(mockedVcs).getFileContent(TEST_MASTER_BRANCH, SCMWorkflow.VER_FILE_NAME);
		IAction action = wf.getProductionReleaseAction();
		assertTrue(action instanceof SCMActionUseLastReleaseVersion);
		SCMActionUseLastReleaseVersion lastRelease = (SCMActionUseLastReleaseVersion) action;
		assertTrue(lastRelease.getVer().equals(ver));
	}

	@Test
	public void testUseLastReleaseIfIgnore() {
		Mockito.doReturn(COMMIT_IGNORE).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		Version ver = new Version("1.0.0");
		Mockito.doReturn("1.0.0").when(mockedVcs).getFileContent(TEST_MASTER_BRANCH, SCMWorkflow.VER_FILE_NAME);
		IAction action = wf.getProductionReleaseAction();
		assertTrue(action instanceof SCMActionUseLastReleaseVersion);
		SCMActionUseLastReleaseVersion lastRelease = (SCMActionUseLastReleaseVersion) action;
		assertTrue(lastRelease.getVer().equals(ver));
	}
}
