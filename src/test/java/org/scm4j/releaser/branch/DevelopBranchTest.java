package org.scm4j.releaser.branch;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;
import org.mockito.Mockito;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.exceptions.EComponentConfig;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

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
	
	@Test
	public void testHasVersionFile() {
		assertTrue(new DevelopBranch(compUnTill).hasVersionFile());
		env.getUnTillVCS().removeFile(compUnTill.getVcsRepository().getDevBranch(), SCMReleaser.VER_FILE_NAME, "version file removed");
		assertFalse(new DevelopBranch(compUnTill).hasVersionFile());
	}

	@Test
	public void testGetMDeps() {
		Component compUnTillDbVersioned = new Component(UNTILL + ":12.13.14");
		MDepsFile mdf = new MDepsFile(Arrays.asList(compUnTillDbVersioned));
		env.getUblVCS().setFileContent(compUBL.getVcsRepository().getDevBranch(), SCMReleaser.MDEPS_FILE_NAME, mdf.toFileContent(),
				"mdeps versioned");
		assertTrue(new DevelopBranch(compUBL).getMDeps().get(0).getVersion().isEmpty());
	}
}
