package org.scm4j.wf.branches;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.NullProgress;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.WorkflowTestBase;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.branch.ReleaseBranchStatus;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.Version;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ReleaseBranchTest extends WorkflowTestBase {
	
	private IProgress nullProgress = new NullProgress();
	private SCMWorkflow wf;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		wf = new SCMWorkflow();
		env.generateFeatureCommit(env.getUnTillDbVCS(), dbUnTillDb.getName(), "feature added");
		env.generateFeatureCommit(env.getUnTillVCS(), dbUnTill.getName(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), dbUBL.getName(), "feature added");
	}
	
	@Test
	public void testMissing() {
		assertEquals(ReleaseBranchStatus.MISSING, rbUnTillDbFixedVer.getStatus());
	}
	
	@Test
	public void testMDEPS_ACTUALIfNoMDeps() throws Exception {
		IAction action = wf.getProductionReleaseAction(compUnTillDb);
		action.execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillDbFixedVer.getStatus());
	}
	
	@Test
	public void testBranched() throws Exception {
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(new ProgressConsole(action.getName(), ">>> ", "<<< "));
		
		// simulate mdeps are not frozen
		MDepsFile mDepsFile = new MDepsFile(Arrays.asList(compUBL.cloneWithDifferentVersion(Version.SNAPSHOT), compUnTillDb.cloneWithDifferentVersion(Version.SNAPSHOT)));
		env.getUnTillVCS().setFileContent(rbUnTillFixedVer.getName(), SCMWorkflow.MDEPS_FILE_NAME, mDepsFile.toFileContent(), "mdeps unversioned");
		assertEquals(ReleaseBranchStatus.BRANCHED, rbUnTillFixedVer.getStatus());
	}
	
	@Test
	public void testMDepsFrozen() {
		// fork all. All release branches must became MDEPS_FROZEN except unTillDb sicne it has no mDeps
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUnTillFixedVer.getStatus());
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUBLFixedVer.getStatus());
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillDbFixedVer.getStatus());
		
		// Build unTillDb and UBL releases. unTill release should became MDEPS_ACTUAL 
		action = wf.getProductionReleaseAction(compUBL);
		action.execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillFixedVer.getStatus());
		
		// add a fix to ACTUAL unTillDb release branch. MDeps of root component should not be MDEPS_ACTUAL anymore because not all dependencies are built.
		env.getUnTillDbVCS().setFileContent(rbUnTillDbFixedVer.getName(), "feature file", "feature line", "feature added");
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUnTillFixedVer.getStatus());
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillDbFixedVer.getStatus());
		
		// build new unTillDb patch. MDeps of root component should still not be actual because we have new unTillDb patch which is not used by upper-level components 
		wf.getProductionReleaseAction(compUnTillDb).execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_FROZEN, rbUnTillFixedVer.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUnTillDbFixedVer.getStatus());
	}
	
	@Test
	public void testMDepsActual() {
		// fork all
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		
		// unTillDb release must be MDEPS_ACTUAL since it has no mDeps
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillDbFixedVer.getStatus());
		
		// Build unTillDb and UBL. unTill release should became MDEPS_ACTUAL 
		action = wf.getProductionReleaseAction(compUBL);
		action.execute(nullProgress);
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rbUnTillFixedVer.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUBLFixedVer.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUnTillDbFixedVer.getStatus());
	}

	@Test
	public void testActual() {
		// fork all
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		
		// build all
		action = wf.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUnTillFixedVer.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUBLFixedVer.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUnTillDbFixedVer.getStatus());
	}
	
	@Test
	public void testLastBuiltSelect() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		ReleaseBranch ethalon = new ReleaseBranch(compUnTillDb, repos);
		
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb, repos));
		
		//fork all 
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(new NullProgress());
		
		// check branch for current release is used
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb, repos));
		
		// build unTillDb
		action = wf.getProductionReleaseAction(compUnTillDb);
		action.execute(new NullProgress());
		
		// check built release version is selected
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb, repos));
	}
	
	@Test
	public void testNextMinorSelect() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		ReleaseBranch ethalon = new ReleaseBranch(compUnTillDb, repos);
		
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb, repos));
		
		//fork all 
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(new NullProgress());
		
		// check branch for current release is used
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb, repos));
		
		// add feature to Develop branch. Unbuild release branch should still be used
		env.generateFeatureCommit(env.getUnTillDbVCS(), dbUnTillDb.getName(), "feature added");
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb, repos));
		
		// build unTillDb
		action = wf.getProductionReleaseAction(compUnTillDb);
		action.execute(new NullProgress());
		
		// check next minor release version is used since we have new commits in Develop Branch
		assertEquals(ethalon.getVersion().toNextMinor(), new ReleaseBranch(compUnTillDb, repos).getVersion());
	}
	
	@Test
	public void testNextPatchSelect() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		ReleaseBranch ethalon = new ReleaseBranch(compUnTillDb, repos);
		
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb, repos));
		
		//fork all 
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(new NullProgress());
		
		// check branch for current release is used
		assertEquals(ethalon, new ReleaseBranch(compUnTillDb, repos));
		
		// build unTillDb
		action = wf.getProductionReleaseAction(compUnTillDb);
		action.execute(new NullProgress());
		
		// add feature to Release Branch. Next patch should be selected
		env.generateFeatureCommit(env.getUnTillDbVCS(), rbUnTillDbFixedVer.getName(), "feature merged");
		assertEquals(ethalon.getVersion().toNextPatch(), new ReleaseBranch(compUnTillDb, repos).getVersion());
	}
	
	@Test
	public void testCertainVersionUsage() {
		Version theVersion = new Version("1.2.3-SNAPSHOT");
		assertEquals(theVersion.toRelease(), new ReleaseBranch(compUBL, theVersion, repos).getVersion());
	}
	
	@Test
	public void testEqualsAndHashCode() {
		EqualsVerifier
				.forClass(ReleaseBranch.class)
				.usingGetClass()
				.withOnlyTheseFields("comp", "name", "version")
				.verify();
	}
}
