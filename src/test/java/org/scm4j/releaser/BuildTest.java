package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.exceptions.ENoReleaseBranchForPatch;
import org.scm4j.releaser.exceptions.ENoReleases;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.WalkDirection;

public class BuildTest extends WorkflowTestBase {
	
	@Test
	public void testFORKIfNoPreviousReleaseBranch() {
		Build mb = new Build(compUnTillDb);
		assertEquals(BuildStatus.FORK, mb.getStatus());
	}
	
	@Test
	public void testFORKIfDevelopModifiedSinceLastBuild() throws Exception {
		// fork unTillDb
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));

		Build mb = new Build(compUnTillDb);
		assertEquals(BuildStatus.DONE, mb.getStatus());
		
		// make Develop modified
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "more feature added");
		
		Build md = new Build(compUnTillDb);
		assertEquals(BuildStatus.FORK, md.getStatus());
	}

	@Test
	public void testFORKIfMDepFORK() throws Exception {
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUBL);
		assertIsGoingToForkAndBuild(action, compUBL);
		action.execute(getProgress(action));
		
		Build mb = new Build(compUBL);
		assertEquals(BuildStatus.DONE, mb.getStatus());
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "unTillDb feature added");
		mb = new Build(compUBL);
		assertEquals(BuildStatus.FORK, mb.getStatus());
	}
	
	@Test
	public void testFORKIfMDepsHasNewerMinors() throws Exception {
		// fork UBL
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUBL); 
		assertIsGoingToForkAndBuild(action, compUBL);
		action.execute(getProgress(action));
		
		// fork unTillDb
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature added");
		action = releaser.getActionTree(compUnTillDb);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		
		// now UBL should be forked because there is a new minor release of unTillDb which is not used by UBL current release
		assertEquals(BuildStatus.FORK, new Build(compUBL).getStatus());
	}
	
	@Test
	public void testDONEIfJustBuilt() throws Exception {
		// fork unTillDb
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		
		assertEquals(BuildStatus.DONE, new Build(compUnTillDb).getStatus());
	}
	
	@Test
	public void testBUILDIfNoReleasesOnExistingReleaseBranch() throws Exception {
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb, ActionSet.FORK_ONLY);
		action.execute(getProgress(action));
		
		Build md = new Build(compUnTillDb);
		assertEquals(BuildStatus.BUILD, md.getStatus());
	}
	
	@Test
	public void testFREEZEIfMDepsNotFrozen() throws Exception {
		// fork UBL
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUBL, ActionSet.FORK_ONLY); 
		assertIsGoingToFork(action, compUBL);
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
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUBL); 
		assertIsGoingToForkAndBuild(action, compUBL);
		action.execute(getProgress(action));
		
		// build unTillDb patch
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), rbUnTillDb.getName(), "patch feature merged");
		action = releaser.getActionTree(compUnTillDb.clone(env.getUnTillDbVer().toRelease()));
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch().toNextPatch(), new ReleaseBranch(compUnTillDb).getVersion());
		
		assertEquals(BuildStatus.ACTUALIZE_PATCHES, new Build(compUBL).getStatus());
	}

	@Test
	public void testPatchDONEIfLastCommitTagged() throws Exception {
		// fork unTillDb
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));

		// add an igonored feature and tag it
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), rbUnTillDb.getName(), LogTag.SCM_IGNORE + " feature megred");
		env.getUnTillDbVCS().createTag(rbUnTillDb.getName(), "tag", "tag", null);

		Options.setIsPatch(true);
		Build b = new Build(compUnTillDb);
		assertEquals(BuildStatus.DONE, b.getStatus());
	}

	@Test
	public void testNoValueableCommitsAfterLastTagInterruption() throws Exception {
		// fork unTillDb
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTillDb);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		action.execute(getProgress(action));

		Component mockedComp = spy(compUnTillDb);
		IVCS mockedVCS = spy(env.getUnTillDbVCS());
		doReturn(mockedVCS).when(mockedComp).getVCS();
		doReturn(new ArrayList<VCSCommit>()).when(mockedVCS)
				.getCommitsRange(anyString(), (String) isNull(), any(WalkDirection.class), anyInt());

		Options.setIsPatch(true);
		Build b = new Build(mockedComp);
		assertEquals(BuildStatus.DONE, b.getStatus());
	}
	
	@Test
	public void testNoReleaseBranchForPatch() throws Exception {
		Options.setIsPatch(true);
		Component comp = new Component(UNTILLDB + ":2.59.0");
		ReleaseBranch rb = new ReleaseBranch(comp, comp.getCoords().getVersion());
		Build b = new Build(rb, comp);
		try {
			b.getStatus();
			fail();
		} catch (ENoReleaseBranchForPatch e) {
		}
	}
	
	@Test
	public void testNoReleasesForPatch() throws Exception {
		IAction action = new SCMReleaser().getActionTree(UNTILLDB, ActionSet.FORK_ONLY);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));
		
		Component comp = new Component(UNTILLDB + ":2.59.0");
		ReleaseBranch rb = new ReleaseBranch(comp, comp.getCoords().getVersion());
		Build b = new Build(rb, comp);
		Options.setIsPatch(true);
		try {
			b.getStatus();
			fail();
		} catch (ENoReleases e) {
		}
		
	}
}
