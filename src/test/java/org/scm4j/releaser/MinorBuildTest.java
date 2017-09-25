package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.internal.verification.checkers.MissingInvocationInOrderChecker;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.CurrentReleaseBranch;
import org.scm4j.releaser.scmactions.SCMActionBuild;
import org.scm4j.releaser.scmactions.SCMActionFork;

public class MinorBuildTest extends WorkflowTestBase {
	
	@Test
	public void testFORKIfNoPreviousReleaseBranch() {
		MinorBuild mb = new MinorBuild(compUnTillDb);
		assertEquals(MinorBuildStatus.FORK, mb.getStatus());
	}
	
	@Test
	public void testFORKIfDevelopModifiedSinceLastBuild() {
		// fork unTillDb
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionFork);
		action.execute(new NullProgress());
		
		// build unTillDb
		action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(new NullProgress());
		
		// make Develop modified
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "more feature added");
		
		MinorBuild md = new MinorBuild(compUnTillDb);
		assertEquals(MinorBuildStatus.FORK, md.getStatus());
	}
	
	@Test
	public void testBUILDIfNoReleasesOnExistingReleaseBranch() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		action.execute(new NullProgress());
		
		MinorBuild md = new MinorBuild(compUnTillDb);
		assertEquals(MinorBuildStatus.BUILD, md.getStatus());
	}
	
	@Test
	public void testFORKIfMDepFORK() {
		// fork UBL
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "ubl feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUBL);
		assertTrue(action instanceof SCMActionFork);
		action.execute(new NullProgress());
		
		// build UBL
		action = releaser.getActionTree(compUBL);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(new NullProgress());
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "unTillDb feature added");
		MinorBuild mb = new MinorBuild(compUBL);
		assertEquals(MinorBuildStatus.FORK, mb.getStatus());
		
		
	}
	
	
	
	
}
