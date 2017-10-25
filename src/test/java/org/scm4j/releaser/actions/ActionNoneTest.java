package org.scm4j.releaser.actions;

import org.junit.Test;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.TestEnvironment;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;

import java.util.ArrayList;

import static org.junit.Assert.*;


public class ActionNoneTest extends WorkflowTestBase {
	
	private static final String TEST_REASON = "test reason";
	
	@Test
	public void testReason() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		ActionNone action = new ActionNone(new ReleaseBranch(comp), new ArrayList<IAction>(),null, TEST_REASON);
		assertEquals(TEST_REASON, action.getReason());
	}

	@Test
	public void testBuildStatus() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		ActionNone action = new ActionNone(new ReleaseBranch(comp), new ArrayList<IAction>(), BuildStatus.BUILD, TEST_REASON);
		assertEquals(BuildStatus.BUILD, action.getMbs());
	}
	
	@Test
	public void testToString() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		ActionNone action = new ActionNone(new ReleaseBranch(comp), new ArrayList<IAction>(), BuildStatus.BUILD, TEST_REASON);
		String toString = action.toString();
		assertTrue(toString.contains(comp.getCoords().toString()));
		assertTrue(toString.contains(BuildStatus.BUILD.toString()));
	}

	@Test
	public void testExecute() {
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		ActionNone action = new ActionNone(new ReleaseBranch(comp), new ArrayList<IAction>(), null, null);
		action.execute(null);
	}

}
