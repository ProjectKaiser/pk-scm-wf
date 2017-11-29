package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class SCMReleaserTest extends WorkflowTestBase {
	
	private final SCMReleaser releaser = new SCMReleaser();

	@Test
	public void testUnsupportedBuildStatus() throws Exception {
		SCMReleaser releaser = spy(new SCMReleaser());
		doReturn(BuildStatus.ERROR).when(releaser).getBuildStatus(any(Component.class), any(CalculatedResult.class));

		try {
			releaser.getActionTree(TestEnvironment.PRODUCT_UNTILL, ActionSet.FULL);
			fail();
		} catch (IllegalArgumentException e) {

		}
	}
	
	@Test
	public void testGetActionTreeUsingActionKind() throws Exception {
		IAction action = releaser.getActionTree(UNTILLDB, ActionSet.FULL);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		
		action = releaser.getActionTree(UNTILLDB, ActionSet.FORK_ONLY);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		
		action = releaser.getActionTree(UNTILLDB, ActionSet.FULL);
		assertIsGoingToBuild(action, compUnTillDb);
		
		action = releaser.getActionTree(UNTILLDB, ActionSet.FORK_ONLY);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD, null, compUnTillDb);
	}
}
