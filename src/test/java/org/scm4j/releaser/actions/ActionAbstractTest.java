package org.scm4j.releaser.actions;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.TestEnvironment;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.exceptions.EReleaserException;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class ActionAbstractTest extends WorkflowTestBase {

	@Test
	public void testExceptions() throws Exception {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		ActionAbstract aa = spy(new ActionAbstract(comp, new ArrayList<>()) {
			@Override
			protected void executeAction(IProgress progress) {

			}

			@Override
			public String toStringAction() {
				return null;
			}
		});
		Exception testException = new Exception("test exeption");
		doThrow(testException).when(aa).executeAction(any(IProgress.class));
		try {
			aa.execute(new ProgressConsole());
		} catch (EReleaserException e) {
			assertEquals(testException, e.getCause());
		}
		EReleaserException testReleaserException = new EReleaserException("test releaser exception");
		doThrow(testReleaserException).when(aa).executeAction(any(IProgress.class));
		try {
			aa.execute(new ProgressConsole());
		} catch (EReleaserException e) {
			assertNull(e.getCause());
		}
	}
}