package org.scm4j.releaser.scmactions;

import org.junit.Before;
import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.TestEnvironment;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.conf.VCSType;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class SCMActionForkTest {

	private Component comp;
	private IProgress progress;

	@Before
	public void setUp() {
		comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		IVCS vcs = mock(IVCS.class);
		doReturn("11.12.13").when(vcs).getFileContent(anyString(), anyString(), anyString());
		VCSRepository repo = new VCSRepository("test repo", "test url", null, VCSType.GIT, "dev branch", "release branch prefix", vcs, null);
		comp.setRepo(repo);
		progress = mock(IProgress.class);
	}
	
	@Test
	public void testWrongBuildStatus() {

		SCMActionFork action;
		int count = 1;
		for (BuildStatus status : BuildStatus.values()) {
			if (status != BuildStatus.FORK && status != BuildStatus.FREEZE) {
				action = new SCMActionFork(new ReleaseBranch(comp), new ArrayList<IAction>(), status);
				try {
					action.execute(progress);
					fail();
				} catch (EReleaserException e) {
					assertTrue(e.getCause() instanceof IllegalStateException);
					verify(progress, times(count)).error(anyString());
					count++;
				}
			}
		}
	}
	
	@Test
	public void testExceptions() throws Exception {
		SCMActionFork action = spy(new SCMActionFork(new ReleaseBranch(comp), new ArrayList<IAction>(), BuildStatus.FREEZE));
		Exception testException = new Exception("test exception");
		EReleaserException testReleaseException = new EReleaserException("test releaser exception");
		doThrow(testException).when(action).freezeMDeps(progress);
		try {
			action.execute(progress);
			fail();
		} catch (EReleaserException e) {
			assertEquals(testException, e.getCause());
			verify(progress).error(anyString());
		}
		
		doThrow(testReleaseException).when(action).freezeMDeps(progress);
		try {
			action.execute(progress);
			fail();
		} catch (EReleaserException e) {
			assertNull(e.getCause());
			verify(progress, times(2)).error(anyString());
		}
	}
}
