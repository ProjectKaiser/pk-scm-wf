package org.scm4j.releaser.actions;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.exceptions.EReleaserException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ActionAbstractTest extends WorkflowTestBase {

	static class TestAction extends ActionAbstract {
		public TestAction(Component comp, List<IAction> childActions, VCSRepository repo) {
			super(comp, childActions, repo);
		}

		@Override
		protected void executeAction(IProgress progress) throws Exception {

		}

		@Override
		public String toStringAction() {
			return null;
		}

		@Override
		public boolean isExecutable() {
			return true;
		}
	}

	@Test
	public void testExceptions() throws Exception {
		ActionAbstract aa = spy(new TestAction(compUnTill, new ArrayList<>(), repoUnTill));
		Exception testException = new Exception("test exeption");
		doThrow(testException).when(aa).executeAction(any(IProgress.class));
		try {
			aa.execute(new ProgressConsole());
			fail();
		} catch (EReleaserException e) {
			assertEquals(testException, e.getCause());
		}
		EReleaserException testReleaserException = new EReleaserException("test releaser exception");
		doThrow(testReleaserException).when(aa).executeAction(any(IProgress.class));
		try {
			aa.execute(new ProgressConsole());
			fail();
		} catch (EReleaserException e) {
			assertNull(e.getCause());
		}
	}

	@Test
	public void testSkipNonExecutableChildActions() {
		IAction doneAction = mock(ActionAbstract.class);
		doReturn(false).when(doneAction).isExecutable();
		doReturn(false).when(doneAction).isUrlProcessed(anyString());

		ActionAbstract aa = spy(new TestAction(compUnTill, Arrays.asList(doneAction), repoUnTill));
		IProgress progress = mock(IProgress.class);
		aa.execute(progress);
		verify(progress, never()).createNestedProgress(anyString());
		verify(doneAction, never()).execute(any(IProgress.class));
	}
}