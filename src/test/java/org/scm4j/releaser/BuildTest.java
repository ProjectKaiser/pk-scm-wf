package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.scmactions.SCMActionBuild;
import org.scm4j.releaser.scmactions.SCMActionFork;

public class BuildTest extends WorkflowTestBase {
	
	@Test
	public void testFORKIfNoPreviousReleaseBranch() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		Build mb = new Build(compUnTillDb);
		assertEquals(BuildStatus.FORK, mb.getStatus());
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
		
		Build md = new Build(compUnTillDb);
		assertEquals(BuildStatus.FORK, md.getStatus());
	}
	
	
	
	@Test
	public void testFORKIfMDepFORK() {
		// fork UBL
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "ubl feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUBL);
		assertTrue(action instanceof SCMActionFork); //TODO: test unTillDb will be forked too because there are no release branches at all
		action.execute(new NullProgress());
		
		// build UBL
		action = releaser.getActionTree(compUBL);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(new NullProgress());
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "unTillDb feature added");
		Build mb = new Build(compUBL);
		assertEquals(BuildStatus.FORK, mb.getStatus());
	}
	
	@Test
	public void testFORKIfMDepsHasNewerMinors() {
		// release UBL
		// fork UBL
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "ubl feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUBL); 
		assertTrue(action instanceof SCMActionFork); // test unTillDb will be forked too because there are no release branches at all
		action.execute(new NullProgress());
		
		// build UBL to avoid skip forking due of CRB.version.patch == 0
		action = releaser.getActionTree(compUBL);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(new NullProgress());
		
		// release a new version of unTillDb
		// fork unTillDb
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionFork);
		action.execute(new NullProgress());
		
		// build unTillDb
		action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(new NullProgress());
		
		// now UBL should be forked because there is a new minor release of unTillDb which is not used by UBL current release
		assertEquals(BuildStatus.FORK, new Build(compUBL).getStatus());
	}
	
	@Test
	public void testNONEIfJustBuilt() {
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
		
		assertEquals(BuildStatus.NONE, new Build(compUnTillDb).getStatus());
	}
	
	@Test
	public void testBUILDIfNoReleasesOnExistingReleaseBranch() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		action.execute(new NullProgress());
		
		Build md = new Build(compUnTillDb);
		assertEquals(BuildStatus.BUILD, md.getStatus());
	}
	
	@Test
	public void testFREEZEIfMDepsNotFrozen() {
		// fork UBL
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "ubl feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUBL); 
		assertTrue(action instanceof SCMActionFork); // test unTillDb will be forked too because there are no release branches at all
		action.execute(new NullProgress());
		
		// simulate mdeps not frozen
		ReleaseBranch crb = new ReleaseBranch(compUBL);
		List<Component> mDeps = crb.getMDeps();
		Component snapshotedUnTillDb = mDeps.get(0).cloneWithDifferentVersion(mDeps.get(0).getVersion().toSnapshot());
		MDepsFile mDepsFile = new MDepsFile(Arrays.asList(snapshotedUnTillDb));
		env.getUblVCS().setFileContent(crb.getName(), SCMReleaser.MDEPS_FILE_NAME, mDepsFile.toFileContent(), "mdeps snapshoted");
		
		assertEquals(BuildStatus.FREEZE, new Build(compUBL).getStatus());
	}
	
	@Test
	@Ignore
	public void testACTUALIZE_PATCHESIfHasNewPatches() {
		// fork UBL
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "ubl feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUBL); 
		assertTrue(action instanceof SCMActionFork); // test unTillDb will be forked too because there are no release branches at all
		action.execute(new NullProgress());
		
		// fork unTillDb
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionFork);
		action.execute(new NullProgress());
		
		// build unTillDb
		action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(new NullProgress());
		
		// build unTillDb patch
		ReleaseBranch crbUnTillDb = new ReleaseBranch(compUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), crbUnTillDb.getName(), "patch feature merged");
		action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(new NullProgress());
		assertEquals(env.getUnTillDbVer().toNextPatch().toNextPatch(), crbUnTillDb.getHeadVersion());
		
		assertEquals(BuildStatus.ACTUALIZE_PATCHES, new Build(compUBL).getStatus());
	}
}
