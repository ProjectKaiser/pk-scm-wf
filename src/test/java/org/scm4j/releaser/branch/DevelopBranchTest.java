package org.scm4j.releaser.branch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;
import org.mockito.Mockito;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.exceptions.EComponentConfig;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;

public class DevelopBranchTest extends WorkflowTestBase {

	@Test
	public void testBranchedIfNothingIsMade() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), dbUnTillDb.getName(), "feature added");
		env.generateFeatureCommit(env.getUnTillVCS(), dbUnTill.getName(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), dbUBL.getName(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(getProgress(action));
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
	
	@Test
	public void testExceptionIfNoVersionFile() {
		env.getUnTillVCS().removeFile(compUnTill.getVcsRepository().getDevBranch(), SCMReleaser.VER_FILE_NAME, "version file deleted");
		try {
			new DevelopBranch(compUnTill).getVersion();
			fail();
		} catch (EComponentConfig e) {
			
		}
			
	}
}
