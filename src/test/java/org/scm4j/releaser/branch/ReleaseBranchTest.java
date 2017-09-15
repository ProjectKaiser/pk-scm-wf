package org.scm4j.releaser.branch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.NullProgress;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ReleaseBranchTest extends WorkflowTestBase {
	
	private static final String CUSTOM_VERSION_STR = "1.2.3";
	private IProgress nullProgress = new NullProgress();
	private SCMReleaser releaser;
	private ReleaseBranch rbUBL;
	private ReleaseBranch rbUnTillDb;
	private ReleaseBranch rbUnTill;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		releaser = new SCMReleaser();
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
		IAction action = releaser.getProductionReleaseAction(compUnTillDb);
		action.execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillDb.getStatus());
	}
	
	@Test
	public void testBranched() throws Exception {
		IAction action = releaser.getProductionReleaseAction(compUnTill);
		action.execute(new ProgressConsole(action.getName(), ">>> ", "<<< "));
		
		// simulate mdeps are not frozen
		MDepsFile mDepsFile = new MDepsFile(Arrays.asList(compUBL.cloneWithDifferentVersion(Version.SNAPSHOT), compUnTillDb.cloneWithDifferentVersion(Version.SNAPSHOT)));
		env.getUnTillVCS().setFileContent(rbUnTill.getName(), SCMReleaser.MDEPS_FILE_NAME, mDepsFile.toFileContent(), "mdeps unversioned");
		assertEquals(ReleaseBranchStatus.BRANCHED, rbUnTill.getStatus());
	}
	
	@Test
	public void testMDepsFrozen() {
		// fork all. All release branches must became MDEPS_FROZEN except unTillDb sicne it has no mDeps
		IAction action = releaser.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUnTill.getStatus());
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUBL.getStatus());
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillDb.getStatus());
		
		// Build unTillDb and UBL releases. unTill release should became MDEPS_ACTUAL 
		action = releaser.getProductionReleaseAction(compUBL);
		action.execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTill.getStatus());
		
		// add a fix to ACTUAL unTillDb release branch. MDeps of root component should not be MDEPS_ACTUAL anymore because not all dependencies are built.
		env.getUnTillDbVCS().setFileContent(rbUnTillDb.getName(), "feature file", "feature line", "feature added");
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUnTill.getStatus());
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillDb.getStatus());
		
		// build new unTillDb patch. MDeps of root component should still not be actual because we have new unTillDb patch which is not used by upper-level components 
		releaser.getProductionReleaseAction(compUnTillDb).execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUnTill.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUnTillDb.getStatus());
	}
	
	@Test
	public void testMDepsActual() {
		// fork all
		IAction action = releaser.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		
		// unTillDb release must be MDEPS_ACTUAL since it has no mDeps
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillDb.getStatus());
		
		// Build unTillDb and UBL. unTill release should became MDEPS_ACTUAL 
		action = releaser.getProductionReleaseAction(compUBL);
		action.execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTill.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUBL.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUnTillDb.getStatus());
	}

	@Test
	public void testActual() {
		// fork all
		IAction action = releaser.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		
		// build all
		action = releaser.getProductionReleaseAction(compUnTill);
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
		IAction action = releaser.getProductionReleaseAction(compUnTill);
		action.execute(new NullProgress());
		
		// check branch for current release is used
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb));
		
		// build unTillDb
		action = releaser.getProductionReleaseAction(compUnTillDb);
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
		IAction action = releaser.getProductionReleaseAction(compUnTill);
		action.execute(new NullProgress());
		
		// check branch for current release is used
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb));
		
		// add feature to Develop branch. Unbuild release branch should still be used
		env.generateFeatureCommit(env.getUnTillDbVCS(), dbUnTillDb.getName(), "feature added");
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb));
		
		// build unTillDb
		action = releaser.getProductionReleaseAction(compUnTillDb);
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
		IAction action = releaser.getProductionReleaseAction(compUnTill);
		action.execute(new NullProgress());
		
		// check branch for current release is used
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb));
		
		// build unTillDb
		action = releaser.getProductionReleaseAction(compUnTillDb);
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
		IAction action = releaser.getProductionReleaseAction(compUnTillDb);
		action.execute(nullProgress);
		
		// build unTillDb
		action = releaser.getProductionReleaseAction(compUnTillDb);
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
		IAction action = releaser.getProductionReleaseAction(compUnTillDb);
		action.execute(nullProgress);
		
		// build unTillDb
		action = releaser.getProductionReleaseAction(compUnTillDb);
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
	
	@Test
	public void testProductComponentUsage() {
		Component compProduct = new Component(UNTILL + ":" + CUSTOM_VERSION_STR, true);
		ReleaseBranch rb = new ReleaseBranch(compProduct);
		assertEquals(CUSTOM_VERSION_STR, rb.getVersion().toString());
	}
}
