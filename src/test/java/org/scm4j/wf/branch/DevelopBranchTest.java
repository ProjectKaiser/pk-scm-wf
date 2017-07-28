package org.scm4j.wf.branch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflowTestBase;

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
}
