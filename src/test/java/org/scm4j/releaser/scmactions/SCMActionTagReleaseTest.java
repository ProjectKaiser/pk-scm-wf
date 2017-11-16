package org.scm4j.releaser.scmactions;

import org.junit.Test;
import org.scm4j.releaser.branch.ReleaseBranch;

import java.util.ArrayList;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class SCMActionTagReleaseTest extends SCMActionTestBase {
	
	@Test
	public void testExceptions() throws Exception {
		comp = spy(comp);
		ReleaseBranch rb = spy(new ReleaseBranch(comp));
		SCMActionTag action = spy(new SCMActionTag(rb, comp, new ArrayList<>()));
		when(comp.getVcsRepository()).thenCallRealMethod().thenReturn(null);
		
		testExceptionThrowing(action, NullPointerException.class);
	}
}
