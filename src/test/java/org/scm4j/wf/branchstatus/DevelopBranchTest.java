package org.scm4j.wf.branchstatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.TestEnvironment;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;

import static org.junit.Assert.assertEquals;

public class DevelopBranchTest {
	
	private TestEnvironment env;
	private VCSRepositories repos;
	
	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		repos = VCSRepositories.loadVCSRepositories();
	}
	
	@After
	public void tearDown() throws Exception {
		env.close();
	}
	
	
	@Test
	public void testBranchedIfNothingIsMade() {
		env.generateLogTag(env.getUnTillVCS(), null, LogTag.SCM_VER);
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL, repos);
		DevelopBranch db = new DevelopBranch(comp);
		DevelopBranchStatus dbs = db.getStatus();
		assertEquals(dbs, DevelopBranchStatus.BRANCHED);
	}
	
	@Test
	public void testModifiedIfHasFeatureCommits() {
		env.generateFeatureCommit(env.getUnTillVCS(), null, "feature commit");
		
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL, repos);
		DevelopBranch db = new DevelopBranch(comp);
		DevelopBranchStatus dbs = db.getStatus();
		assertEquals(dbs, DevelopBranchStatus.MODIFIED);
	}
	
	@Test
	public void testIgnored() {
		env.generateLogTag(env.getUnTillVCS(), null, LogTag.SCM_IGNORE);
		
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILL, repos);
		DevelopBranch db = new DevelopBranch(comp);
		DevelopBranchStatus dbs = db.getStatus();
		assertEquals(dbs, DevelopBranchStatus.IGNORED);
	}
}
