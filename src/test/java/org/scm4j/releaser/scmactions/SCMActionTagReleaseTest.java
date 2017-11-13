package org.scm4j.releaser.scmactions;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;

public class SCMActionTagReleaseTest extends SCMActionTestBase {
	
	@Test
	public void testExceptions() throws Exception {
		comp = spy(comp);
		ReleaseBranch rb = spy(new ReleaseBranch(comp));
		SCMActionTag action = spy(new SCMActionTag(rb, comp, new ArrayList<IAction>()));
		when(comp.getVcsRepository()).thenCallRealMethod().thenReturn(null);
		
		testExceptionThrowing(action, NullPointerException.class);
	}
}
