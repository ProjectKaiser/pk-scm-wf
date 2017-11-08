package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.IAction;

public class SCMReleaserTest extends WorkflowTestBase {
	
	private final SCMReleaser releaser = new SCMReleaser();

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
