package org.scm4j.wf.branch;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.wf.NullProgress;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.WorkflowTestBase;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.branch.ReleaseBranchStatus;
import org.scm4j.wf.conf.MDepsFile;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

public class ReleaseBranchTest extends WorkflowTestBase {
	
	private IProgress nullProgress = new NullProgress();
	private SCMWorkflow wf;
	private ReleaseBranch rbUBL;
	private ReleaseBranch rbUnTillDb;
	private ReleaseBranch rbUnTill;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		wf = new SCMWorkflow();
		env.generateFeatureCommit(env.getUnTillDbVCS(), dbUnTillDb.getName(), "feature added");
		env.generateFeatureCommit(env.getUnTillVCS(), dbUnTill.getName(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), dbUBL.getName(), "feature added");
		rbUBL = new ReleaseBranch(compUBL);
		rbUnTillDb = new ReleaseBranch(compUnTillDb);
		rbUnTill = new ReleaseBranch(compUnTill);
	}
	
	@Test
	public void testMissing() {
		assertEquals(ReleaseBranchStatus.MISSING, rbUnTillDb.getStatus());
	}
	
	@Test
	public void testMDEPS_ACTUALIfNoMDeps() throws Exception {
		IAction action = wf.getProductionReleaseAction(compUnTillDb);
		action.execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillDb.getStatus());
	}
	
	@Test
	public void testBranched() throws Exception {
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(new ProgressConsole(action.getName(), ">>> ", "<<< "));
		
		// simulate mdeps are not frozen
		MDepsFile mDepsFile = new MDepsFile(Arrays.asList(compUBL.cloneWithDifferentVersion(Version.SNAPSHOT), compUnTillDb.cloneWithDifferentVersion(Version.SNAPSHOT)));
		env.getUnTillVCS().setFileContent(rbUnTill.getName(), SCMWorkflow.MDEPS_FILE_NAME, mDepsFile.toFileContent(), "mdeps unversioned");
		assertEquals(ReleaseBranchStatus.BRANCHED, rbUnTill.getStatus());
	}
	
	@Test
	public void testMDepsFrozen() {
		// fork all. All release branches must became MDEPS_FROZEN except unTillDb sicne it has no mDeps
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUnTill.getStatus());
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUBL.getStatus());
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillDb.getStatus());
		
		// Build unTillDb and UBL releases. unTill release should became MDEPS_ACTUAL 
		action = wf.getProductionReleaseAction(compUBL);
		action.execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTill.getStatus());
		
		// add a fix to ACTUAL unTillDb release branch. MDeps of root component should not be MDEPS_ACTUAL anymore because not all dependencies are built.
		env.getUnTillDbVCS().setFileContent(rbUnTillDb.getName(), "feature file", "feature line", "feature added");
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUnTill.getStatus());
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillDb.getStatus());
		
		// build new unTillDb patch. MDeps of root component should still not be actual because we have new unTillDb patch which is not used by upper-level components 
		wf.getProductionReleaseAction(compUnTillDb).execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUnTill.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUnTillDb.getStatus());
	}
	
	@Test
	public void testMDepsActual() {
		// fork all
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		
		// unTillDb release must be MDEPS_ACTUAL since it has no mDeps
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillDb.getStatus());
		
		// Build unTillDb and UBL. unTill release should became MDEPS_ACTUAL 
		action = wf.getProductionReleaseAction(compUBL);
		action.execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTill.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUBL.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUnTillDb.getStatus());
	}

	@Test
	public void testActual() {
		// fork all
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		
		// build all
		action = wf.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUnTill.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUBL.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUnTillDb.getStatus());
	}
	
	@Test
	public void testLastBuiltSelect() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		ReleaseBranch ethalon = new ReleaseBranch(compUnTillDb);
		
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb));
		
		//fork all 
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(new NullProgress());
		
		// check branch for current release is used
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb));
		
		// build unTillDb
		action = wf.getProductionReleaseAction(compUnTillDb);
		action.execute(new NullProgress());
		
		// check built release version is selected
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb));
	}
	
	@Test
	public void testNextMinorSelect() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		ReleaseBranch ethalon = new ReleaseBranch(compUnTillDb);
		
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb));
		
		//fork all 
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(new NullProgress());
		
		// check branch for current release is used
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb));
		
		// add feature to Develop branch. Unbuild release branch should still be used
		env.generateFeatureCommit(env.getUnTillDbVCS(), dbUnTillDb.getName(), "feature added");
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb));
		
		// build unTillDb
		action = wf.getProductionReleaseAction(compUnTillDb);
		action.execute(new NullProgress());
		
		// check next minor release version is used since we have new commits in Develop Branch
		assertEquals(ethalon.getVersion().toNextMinor(), new ReleaseBranch(compUnTillDb).getVersion());
	}
	
	@Test
	public void testNextPatchSelect() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		ReleaseBranch ethalon = new ReleaseBranch(compUnTillDb);
		
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb));
		
		//fork all 
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(new NullProgress());
		
		// check branch for current release is used
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb));
		
		// build unTillDb
		action = wf.getProductionReleaseAction(compUnTillDb);
		action.execute(new NullProgress());
		
		// add feature to Release Branch. Next patch should be selected
		env.generateFeatureCommit(env.getUnTillDbVCS(), rbUnTillDb.getName(), "feature merged");
		assertEquals(ethalon.getVersion().toNextPatch(), new ReleaseBranch(compUnTillDb).getVersion());
	}
	
	@Test
	public void testCertainVersionUsage() {
		Version theVersion = new Version("1.2.3-SNAPSHOT");
		assertEquals(theVersion.toRelease(), new ReleaseBranch(compUBL, theVersion).getVersion());
	}
	
	@Test
	public void testEqualsAndHashCode() {
		EqualsVerifier
				.forClass(ReleaseBranch.class)
				.usingGetClass()
				.withOnlyTheseFields("comp", "name", "version")
				.verify();
	}
	
	@Test
	public void testToString() {
		ReleaseBranch rb = new ReleaseBranch(compUnTillDb);
		assertTrue(rb.toString().contains(compUnTillDb.getName()));
	}
	
	@Test
	public void testPreHeadIsNotTaggedIfPreHeadMissing() {
		// fork unTillDb
		IAction action = wf.getProductionReleaseAction(compUnTillDb);
		action.execute(nullProgress);
		
		// build unTillDb
		action = wf.getProductionReleaseAction(compUnTillDb);
		action.execute(nullProgress);
		
		ReleaseBranch rb = spy(new ReleaseBranch(compUnTillDb));
		assertEquals(ReleaseBranchStatus.ACTUAL, rb.getStatus());
		VCSCommit commit = VCSCommit.EMPTY;
		doReturn(Arrays.asList(commit)).when(rb).getLast2Commits();
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rb.getStatus());
	}
	
	@Test
	public void testMDEPS_ACTUALIfWrongTagOnPreHead() {
		// fork unTillDb
		IAction action = wf.getProductionReleaseAction(compUnTillDb);
		action.execute(nullProgress);
		
		// build unTillDb
		action = wf.getProductionReleaseAction(compUnTillDb);
		action.execute(nullProgress);
		
		List<VCSTag> tags = env.getUnTillDbVCS().getTags();
		assertTrue(tags.size() == 1);
		VCSTag tag = tags.get(0);
		String taggedRev = tag.getRelatedCommit().getRevision();
		
		env.getUnTillDbVCS().removeTag(tag.getTagName());
		
		ReleaseBranch rb = new ReleaseBranch(compUnTillDb);
		env.getUnTillDbVCS().createTag(rb.getName(), "wrong_tag", "", taggedRev);
		
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rb.getStatus());
	}
}
