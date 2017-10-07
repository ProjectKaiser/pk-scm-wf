package org.scm4j.releaser.scmactions;

import org.junit.Test;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.exceptions.EReleaserException;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class SCMActionForkTest extends SCMActionTestBase {

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
		testExceptionThrowing(action, testException);

		doThrow(testReleaseException).when(action).freezeMDeps(progress);
		testExceptionThrowing(action, testReleaseException);
	}
}
