package org.scm4j.wf.branches;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;
import org.mockito.Mockito;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.NullProgress;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.WorkflowTestBase;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.DevelopBranch;
import org.scm4j.wf.branch.DevelopBranchStatus;
import org.scm4j.wf.conf.Component;

public class DevelopBranchTest extends WorkflowTestBase {

	@Test
	public void testBranchedIfNothingIsMade() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), dbUnTillDb.getName(), "feature added");
		env.generateFeatureCommit(env.getUnTillVCS(), dbUnTill.getName(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), dbUBL.getName(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(new NullProgress());
		assertEquals(DevelopBranchStatus.BRANCHED, dbUnTill.getStatus());
	}

	@Test
	public void testModifiedIfHasFeatureCommits() {
		env.generateFeatureCommit(env.getUnTillVCS(), null, "feature commit");
		assertEquals(DevelopBranchStatus.MODIFIED, dbUnTill.getStatus());
	}

	@Test
	public void testIgnored() {
		env.generateFeatureCommit(env.getUnTillVCS(), null, "feature commit");
		env.generateLogTag(env.getUnTillVCS(), null, LogTag.SCM_IGNORE);
		assertEquals(DevelopBranchStatus.IGNORED, dbUnTill.getStatus());
	}
	
	@Test
	public void testToString() {
		assertNotNull(dbUnTillDb.toString());
	}
	
	@Test
	public void testIGNOREDIfNoCommits() throws GitAPIException {
		Component compFromEmptyRepo = Mockito.spy(new Component("eu.untill:comp-from-empty-repo"));
		IVCS mockedVCS = Mockito.mock(IVCS.class);
		Mockito.doReturn(mockedVCS).when(compFromEmptyRepo).getVCS();
		Mockito.doReturn(new ArrayList<VCSCommit>()).when(mockedVCS).log(Mockito.anyString(), Mockito.anyInt());
		assertEquals(DevelopBranchStatus.IGNORED, new DevelopBranch(compFromEmptyRepo).getStatus());
	}
}
