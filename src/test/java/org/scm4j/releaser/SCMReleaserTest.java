package org.scm4j.releaser;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;

public class SCMReleaserTest extends WorkflowTestBase {
	
	private final SCMReleaser releaser = new SCMReleaser();

	@Test
	public void testUnsupportedBuildStatus() throws Exception {
		SCMReleaser releaser = spy(new SCMReleaser());
		doReturn(BuildStatus.ERROR).when(releaser).getBuildStatus(any(Component.class), any(CalculatedResult.class), any(IProgress.class));

		try {
			releaser.getActionTree(TestEnvironment.PRODUCT_UNTILL, ActionKind.FULL);
			fail();
		} catch (IllegalArgumentException e) {

		}
	}
	
	@Test
	public void testGetActionTreeUsingActionKind() throws Exception {
		IAction action = releaser.getActionTree(UNTILLDB, ActionKind.FULL);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		
		action = releaser.getActionTree(UNTILLDB, ActionKind.FORK_ONLY);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		
		action = releaser.getActionTree(UNTILLDB, ActionKind.FULL);
		assertIsGoingToBuild(action, compUnTillDb);
		
		action = releaser.getActionTree(UNTILLDB, ActionKind.FORK_ONLY);
		assertIsGoingToDoNothing(action, BuildStatus.BUILD, null, compUnTillDb);
	}
}
