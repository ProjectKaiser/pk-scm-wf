package org.scm4j.releaser.actions;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.CurrentReleaseBranch;
import org.scm4j.releaser.TestEnvironment;
import org.scm4j.releaser.conf.Component;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


public class ActionNoneTest {
	
	private static final String TEST_EXCEPTION = "test exception";
	private static final String TEST_REASON = "test reason";

	@Test
	public void testNestedActionException() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		IAction mockedNestedAction = mock(IAction.class);
		RuntimeException testException = new RuntimeException(TEST_EXCEPTION);
		doThrow(testException).when(mockedNestedAction).execute(any(IProgress.class));
		ActionNone action = new ActionNone(new CurrentReleaseBranch(comp), Collections.singletonList(mockedNestedAction), TEST_REASON);
		IProgress mockedProgress = mock(IProgress.class);
		try {
			action.execute(mockedProgress);
			fail();
		} catch (RuntimeException e) {
			assertEquals(e.getCause(), testException);
		}
		verify(mockedProgress).error(anyString());
	}
	
	@Test
	public void testReason() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		ActionNone action = new ActionNone(new CurrentReleaseBranch(comp), new ArrayList<IAction>(), TEST_REASON);
		assertEquals(TEST_REASON, action.getReason());
	}
	
	@Test
	public void testToString() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		ActionNone action = new ActionNone(new CurrentReleaseBranch(comp), new ArrayList<IAction>(), TEST_REASON);
		String toString = action.toString();
		assertTrue(toString.contains(comp.getCoords().toString()));
	}

}
