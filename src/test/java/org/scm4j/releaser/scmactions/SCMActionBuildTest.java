package org.scm4j.releaser.scmactions;

import java.util.ArrayList;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.TestEnvironment;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.conf.VCSType;
import org.scm4j.releaser.exceptions.ENoBuilder;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SCMActionBuildTest {
	
	@Test
	public void testNoBuilder() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		IVCS vcs = mock(IVCS.class);
		doReturn("11.12.13").when(vcs).getFileContent(anyString(), anyString(), anyString());
		VCSRepository repo = new VCSRepository("test repo", "test url", null, VCSType.GIT, "dev branch", "release branch prefix", vcs, null);
		comp.setRepo(repo);
		SCMActionBuild action = new SCMActionBuild(new ReleaseBranch(comp), new ArrayList<IAction>(), BuildStatus.BUILD);
		IProgress progress = mock(IProgress.class);
		try {
			action.execute(progress);
			fail();
		} catch (ENoBuilder e) {
			assertEquals(comp, e.getComp());
			verify(progress).error(anyString());
		}
	}
	
	@Test
	public void testWrongBuildStatus() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		IVCS vcs = mock(IVCS.class);
		doReturn("11.12.13").when(vcs).getFileContent(anyString(), anyString(), anyString());
		VCSRepository repo = new VCSRepository("test repo", "test url", null, VCSType.GIT, "dev branch", "release branch prefix", vcs, null);
		comp.setRepo(repo);
		IProgress progress = mock(IProgress.class);
		SCMActionBuild action;
		int count = 1;
		for (BuildStatus status : BuildStatus.values()) {
			if (status != BuildStatus.BUILD_MDEPS && status != BuildStatus.ACTUALIZE_PATCHES && status != BuildStatus.BUILD) {
				action = new SCMActionBuild(new ReleaseBranch(comp), new ArrayList<IAction>(), status);
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
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		IVCS vcs = mock(IVCS.class);
		doReturn("11.12.13").when(vcs).getFileContent(anyString(), anyString(), anyString());
		VCSRepository repo = new VCSRepository("test repo", "test url", null, VCSType.GIT, "dev branch", "release branch prefix", vcs, null);
		comp.setRepo(repo);
		IProgress progress = mock(IProgress.class);
		SCMActionBuild action = spy(new SCMActionBuild(new ReleaseBranch(comp), new ArrayList<IAction>(), BuildStatus.ACTUALIZE_PATCHES));
		Exception testException = new Exception("test exception");
		EReleaserException testReleaseException = new EReleaserException("test releaser exception");
		doThrow(testException).when(action).actualizePatches(progress);
		try {
			action.execute(progress);
			fail();
		} catch (EReleaserException e) {
			assertEquals(testException, e.getCause());
			verify(progress).error(anyString());
		}
		
		doThrow(testReleaseException).when(action).actualizePatches(progress);
		try {
			action.execute(progress);
			fail();
		} catch (EReleaserException e) {
			assertNull(e.getCause());
			verify(progress, times(2)).error(anyString());
		}
	}

}
