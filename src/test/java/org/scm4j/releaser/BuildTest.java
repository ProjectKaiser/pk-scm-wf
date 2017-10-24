package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.exceptions.ENoReleaseBranchForPatch;
import org.scm4j.releaser.exceptions.ENoReleases;
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
	public void testFORKIfDevelopModifiedSinceLastBuild() throws Exception {
		// fork unTillDb
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionFork);
		action.execute(getProgress(action));
		
		// build unTillDb
		action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(getProgress(action));
		
		// make Develop modified
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "more feature added");
		
		Build md = new Build(compUnTillDb);
		assertEquals(BuildStatus.FORK, md.getStatus());
	}
	
	
	
	@Test
	public void testFORKIfMDepFORK() throws Exception {
		// fork UBL
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "ubl feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUBL);
		assertTrue(action instanceof SCMActionFork);
		action.execute(getProgress(action));
		
		// build UBL
		action = releaser.getActionTree(compUBL);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(getProgress(action));
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "unTillDb feature added");
		Build mb = new Build(compUBL);
		assertEquals(BuildStatus.FORK, mb.getStatus());
	}
	
	@Test
	public void testFORKIfMDepsHasNewerMinors() throws Exception {
		// release UBL
		// fork UBL
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "ubl feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUBL); 
		assertTrue(action instanceof SCMActionFork); // test unTillDb will be forked also because there are no release branches at all
		action.execute(getProgress(action)); // fork 2.59.0 untilldb
		
		// build UBL to avoid skip forking due of rb.version.patch == 0 (fork needed? -> 2-nd line -> no)
		action = releaser.getActionTree(compUBL);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(getProgress(action)); // build 2.59.0 untilldb
		
		// release a new version of unTillDb
		// fork unTillDb
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionFork);
		action.execute(getProgress(action));
		
		// build unTillDb
		action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(getProgress(action));
		
		// now UBL should be forked because there is a new minor release of unTillDb which is not used by UBL current release
		assertEquals(BuildStatus.FORK, new Build(compUBL).getStatus());
	}
	
	@Test
	public void testNONEIfJustBuilt() throws Exception {
		// fork unTillDb
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionFork);
		action.execute(getProgress(action));
		
		// build unTillDb
		action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(getProgress(action));
		
		assertEquals(BuildStatus.DONE, new Build(compUnTillDb).getStatus());
	}
	
	@Test
	public void testBUILDIfNoReleasesOnExistingReleaseBranch() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		action.execute(getProgress(action));
		
		Build md = new Build(compUnTillDb);
		assertEquals(BuildStatus.BUILD, md.getStatus());
	}
	
	@Test
	public void testFREEZEIfMDepsNotFrozen() throws Exception {
		// fork UBL
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "ubl feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUBL); 
		assertTrue(action instanceof SCMActionFork); // test unTillDb will be forked too because there are no release branches at all
		action.execute(getProgress(action));
		
		// simulate mdeps not frozen
		ReleaseBranch rb = new ReleaseBranch(compUBL);
		List<Component> mDeps = rb.getMDeps();
		Component snapshotedUnTillDb = mDeps.get(0).clone(mDeps.get(0).getVersion().toSnapshot());
		MDepsFile mDepsFile = new MDepsFile(Arrays.asList(snapshotedUnTillDb));
		env.getUblVCS().setFileContent(rb.getName(), SCMReleaser.MDEPS_FILE_NAME, mDepsFile.toFileContent(), "mdeps snapshoted");
		
		assertEquals(BuildStatus.FREEZE, new Build(compUBL).getStatus());
	}
	
	@Test
	public void testACTUALIZE_PATCHESIfHasNewPatches() throws Exception {
		// fork UBL
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "ubl feature added");
		
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUBL); 
		assertTrue(action instanceof SCMActionFork); // test unTillDb will be forked too because there are no release branches at all
		action.execute(getProgress(action));
		
		// build unTillDb
		action = releaser.getActionTree(compUnTillDb);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(getProgress(action));
		
		// build unTillDb patch
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), rbUnTillDb.getName(), "patch feature merged");
		action = releaser.getActionTree(compUnTillDb.clone(env.getUnTillDbVer().toRelease()));
		Options.setIsPatch(false);
		assertTrue(action instanceof SCMActionBuild);
		action.execute(getProgress(action));
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch().toNextPatch(), new ReleaseBranch(compUnTillDb).getVersion());
		
		assertEquals(BuildStatus.ACTUALIZE_PATCHES, new Build(compUBL).getStatus());
	}
	
	@Test
	public void testExceptions() throws Exception {
		Options.setIsPatch(true);
		Component comp = new Component(UNTILLDB + ":2.59.0");
		ReleaseBranch rb = new ReleaseBranch(comp, comp.getCoords().getVersion());
		Build b = new Build(rb);
		try {
			b.getStatus();
			fail();
		} catch (ENoReleaseBranchForPatch e) {
		}
		
		IAction action = new SCMReleaser().getActionTree(UNTILLDB);
		assertTrue(action instanceof SCMActionFork);
		action.execute(getProgress(action));
		
		Options.setIsPatch(true);
		try {
			b.getStatus();
			fail();
		} catch (ENoReleases e) {
		}
		
	}
}
