package org.scm4j.wf;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.ReleaseBranchStatus;
import org.scm4j.wf.conf.MDepsFile;

public class ReleaseBranchTest extends SCMWorkflowTestBase {
	
	private IProgress nullProgress = new NullProgress();
	private SCMWorkflow wf;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		env.generateFeatureCommit(env.getUnTillDbVCS(), dbUnTillDb.getName(), "feature added");
		env.generateFeatureCommit(env.getUnTillVCS(), dbUnTill.getName(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), dbUBL.getName(), "feature added");
		wf = new SCMWorkflow();
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
		MDepsFile mDepsFile = new MDepsFile(Arrays.asList(compUBL, compUnTillDb));
		env.getUnTillVCS().setFileContent(rbUnTillFixedVer.getReleaseBranchName(), SCMWorkflow.MDEPS_FILE_NAME, mDepsFile.toFileContent(), "mdeps unversioned");
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
		env.getUnTillDbVCS().setFileContent(rbUnTillDbFixedVer.getReleaseBranchName(), "feature file", "feature line", "feature added");
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
}
