package org.scm4j.releaser;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Map;

import org.junit.Test;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.branch.ReleaseBranch;

public class SCMReleaserTest extends WorkflowTestBase {

	@SuppressWarnings("unchecked")
	@Test
	public void testUnsupportedBuildStatus() {
		SCMReleaser releaser = spy(new SCMReleaser());
		doReturn(BuildStatus.ERROR).when(releaser).getBuildStatus(any(Map.class), any(ReleaseBranch.class));

		try {
			releaser.getActionTree(TestEnvironment.PRODUCT_UNTILL, ActionKind.AUTO);
			fail();
		} catch (IllegalArgumentException e) {

		}
	}
}
