package org.scm4j.wf;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.scm4j.wf.branch.DevelopBranch;
import org.scm4j.wf.branch.DevelopBranchStatus;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.branch.ReleaseBranchStatus;

public class DevelopBranchTest extends SCMWorkflowTestBase {

	@Test
	public void testBranchedIfNothingIsMade() {
		env.generateLogTag(env.getUnTillVCS(), null, LogTag.SCM_VER);
		DevelopBranchStatus dbs = dbUnTill.getStatus();
		assertEquals(dbs, DevelopBranchStatus.BRANCHED);
	}

	@Test
	public void testModifiedIfHasFeatureCommits() {
		env.generateFeatureCommit(env.getUnTillVCS(), null, "feature commit");
		DevelopBranchStatus dbs = dbUnTill.getStatus();
		assertEquals(dbs, DevelopBranchStatus.MODIFIED);
	}

	@Test
	public void testIgnored() {
		env.generateLogTag(env.getUnTillVCS(), null, LogTag.SCM_IGNORE);
		DevelopBranchStatus dbs = dbUnTill.getStatus();
		assertEquals(dbs, DevelopBranchStatus.IGNORED);
	}
	
	@Test
	public void testGetCurrentReleaseBranchMissing() {
		DevelopBranch db = new DevelopBranch(compUnTillDb);
		ReleaseBranch rb = db.getCurrentReleaseBranch(repos);
		assertEquals(ReleaseBranchStatus.MISSING, rb.getStatus());
		assertEquals(env.getUnTillDbVer().toReleaseString(), rb.getVersion().toReleaseString());
	}
	
	@Test
	public void testGetCurrentReleaseBranchMISSINGAfterCompleted() {
		env.generateRelease(compUnTillDb.cloneWithDifferentVersion(env.getUnTillDbVer().toString()));
		env.generateFeatureCommit(env.getUnTillDbVCS(), null, "feature added");
		
		DevelopBranch db = new DevelopBranch(compUnTillDb);
		ReleaseBranch rb = db.getCurrentReleaseBranch(repos);
		assertEquals(ReleaseBranchStatus.MISSING, rb.getStatus());
		assertEquals(env.getUnTillDbVer().toNextMinor().toReleaseString(), rb.getVersion().toReleaseString());
	}
	
	@Test
	public void testGetCurrentReleaseBranchUncompletedNoMDeps() throws InterruptedException {
		env.getUnTillDbVCS().createBranch(dbUnTillDb.getName(), rbUnTillDbFixedVer.getReleaseBranchName(), null);
		env.getUnTillDbVCS().setFileContent(rbUnTillDbFixedVer.getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME, env.getUnTillDbVer().toReleaseString(), LogTag.SCM_VER);
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), null, "feature added");
		
		DevelopBranch db = new DevelopBranch(compUnTillDb);
		ReleaseBranch rb = db.getCurrentReleaseBranch(repos);
		assertEquals(ReleaseBranchStatus.MDEPS_ACTUAL, rb.getStatus());
		assertEquals(env.getUnTillDbVer().toReleaseString(), rb.getVersion().toReleaseString());
	}
	
	@Test
	public void testGetCurrentReleaseBranchUncompletedHasMDeps() {
		env.generateRelease(compUBL.cloneWithDifferentVersion(env.getUblVer().toString()));
		env.generateFeatureCommit(env.getUblVCS(), null, "feature added");
		env.getUblVCS().removeTag(env.getUblVer().toReleaseString());
		
		DevelopBranch db = new DevelopBranch(compUBL);
		ReleaseBranch rb = db.getCurrentReleaseBranch(repos);
		assertEquals(ReleaseBranchStatus.BRANCHED, rb.getStatus());
		assertEquals(env.getUblVer().toReleaseString(), rb.getVersion().toReleaseString());
	}

}
