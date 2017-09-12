package org.scm4j.wf.actions;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.TestEnvironment;
import org.scm4j.wf.conf.Component;


public class ActionNoneTest {
	
	private static final String TEST_EXCEPTION = "test exception";
	private static final String TEST_REASON = "test reason";

	@Test
	public void testNestedActionException() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		IAction mockedNestedAction = mock(IAction.class);
		RuntimeException testException = new RuntimeException(TEST_EXCEPTION);
		doThrow(testException).when(mockedNestedAction).execute(any(IProgress.class));
		ActionNone action = new ActionNone(comp, Arrays.asList(mockedNestedAction), TEST_REASON);
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
		ActionNone action = new ActionNone(comp, new ArrayList<IAction>(), TEST_REASON);
		assertEquals(TEST_REASON, action.getReason());
	}
	
	@Test
	public void testToString() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		ActionNone action = new ActionNone(comp, new ArrayList<IAction>(), TEST_REASON);
		String toString = action.toString();
		assertTrue(toString.contains(comp.getCoords().toString()));
	}

}
