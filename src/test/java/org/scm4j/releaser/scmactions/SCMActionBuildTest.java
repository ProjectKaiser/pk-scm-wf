package org.scm4j.releaser.scmactions;

import org.junit.Test;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.exceptions.ENoBuilder;
import org.scm4j.releaser.exceptions.EReleaserException;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SCMActionBuildTest extends SCMActionTestBase {

	@Test
	public void testNoBuilder() {
		SCMActionBuild action = new SCMActionBuild(new ReleaseBranch(comp), new ArrayList<IAction>(), BuildStatus.BUILD);
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
		SCMActionBuild action;
		for (BuildStatus status : BuildStatus.values()) {
			if (status != BuildStatus.BUILD_MDEPS && status != BuildStatus.ACTUALIZE_PATCHES && status != BuildStatus.BUILD) {
				action = new SCMActionBuild(new ReleaseBranch(comp), new ArrayList<IAction>(), status);
				testExceptionThrowing(action, IllegalStateException.class);
			}
		}
	}

	@Test
	public void testExceptions() throws Exception {
		SCMActionBuild action = spy(new SCMActionBuild(new ReleaseBranch(comp), new ArrayList<IAction>(), BuildStatus.ACTUALIZE_PATCHES));
		Exception testException = new Exception("test exception");
		EReleaserException testReleaseException = new EReleaserException("test releaser exception");
		doThrow(testException).when(action).actualizePatches(progress);
		testExceptionThrowing(action, testException);

		doThrow(testReleaseException).when(action).actualizePatches(progress);
		testExceptionThrowing(action, testReleaseException);
	}
}
