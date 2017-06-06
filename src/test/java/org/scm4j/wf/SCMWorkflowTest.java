package org.scm4j.wf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
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
import org.scm4j.wf.model.Dep;
import org.scm4j.wf.model.VCSRepository;
import org.scm4j.wf.model.VCSType;

@PrepareForTest(VCSFactory.class)
@RunWith(PowerMockRunner.class)
public class SCMWorkflowTest {

	private static final String TEST_DEP = "test:dep";

	private static final String TEST_MASTER_BRANCH = "test master branch";

	@Mock
	IVCS mockedVcs;
	
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
		PowerMockito.when(VCSFactory.getIVCS(testRepo)).thenReturn(mockedVcs);
		wf = new SCMWorkflow(TEST_DEP, vcsRepos);
		Mockito.doReturn(true).when(mockedVcs).fileExists(testRepo.getDevBranch(), SCMWorkflow.VER_FILE_NAME);
	}
	
	@Test
	public void testProductionReleaseNewFeatures() {
		VCSCommit featureCommit = new VCSCommit("test revision", "", null);
		Mockito.doReturn(featureCommit).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		IAction action = wf.getProductionReleaseAction();
		assertTrue(action instanceof SCMActionProductionRelease);
		SCMActionProductionRelease r = (SCMActionProductionRelease) action;
		assertEquals(r.getReason(), ProductionReleaseReason.NEW_FEATURES);
	}
	
	@Test 
	public void testActionError() {
		Mockito.doReturn(false).when(mockedVcs).fileExists(testRepo.getDevBranch(), SCMWorkflow.VER_FILE_NAME);
		VCSCommit headCommit = new VCSCommit("test revision", "", null);
		Mockito.doReturn(headCommit).when(mockedVcs).getHeadCommit(TEST_MASTER_BRANCH);
		IAction action = wf.getProductionReleaseAction();
		assertTrue(action instanceof ActionError);
		assertNotNull(((ActionError) action).getCause());
	}
	
	@Test
	public void testActionNone() {
		//test none
		List<Dep> testMDeps = Arrays.asList(new Dep(TEST_DEP + ":1.0.0", vcsRepos));
		wf.setMDeps(testMDeps);
		wf.setChildActions(Arrays.asList((IAction) new ActionError(testRepo, Collections.<IAction>emptyList(), TEST_MASTER_BRANCH, "test cause")));
		IAction res = wf.getAction();
		assertTrue(res instanceof ActionNone);
	}
	
	@Test
	public void testProductionReleaseNewDependencies() {
		Mockito.doReturn("0.0.0").when(mockedVcs).getFileContent(TEST_MASTER_BRANCH, SCMWorkflow.VER_FILE_NAME);
		List<Dep> testMDeps = Arrays.asList(new Dep(TEST_DEP + ":1.0.0", vcsRepos));
		wf.setMDeps(testMDeps);
		wf.setChildActions(Arrays.<IAction>asList(new SCMActionUseLastReleaseVersion(testRepo, Collections.<IAction>emptyList(), TEST_MASTER_BRANCH)));
		IAction action = wf.getAction();
		assertTrue(action instanceof SCMActionProductionRelease);
		SCMActionProductionRelease pr = (SCMActionProductionRelease) action;
		assertEquals(pr.getReason(), ProductionReleaseReason.NEW_DEPENDENCIES);
	}
}
