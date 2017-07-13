package org.scm4j.wf;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;

public class BranchStructureTest {

	private static final String TEST_BRANCH = "test branch";
	
	@Mock
	IVCS vcs;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testHasFeatures() {
		VCSCommit commit = new VCSCommit("rev", "last head commit with no tags", "author");
		Mockito.doReturn(commit).when(vcs).getHeadCommit(TEST_BRANCH);
		BranchStructure bs = new BranchStructure(vcs, TEST_BRANCH);
		assertTrue(bs.getHasFeatures());
	}

	@Test
	public void testHasNoFeatures() {
		VCSCommit commit = new VCSCommit("rev",
				"head commit with " + LogTag.SCM_IGNORE + " tag", "author");
		Mockito.doReturn(commit).when(vcs).getHeadCommit(TEST_BRANCH);
		BranchStructure bs = new BranchStructure(vcs, TEST_BRANCH);
		assertFalse(bs.getHasFeatures());

		commit = new VCSCommit("rev", "head commit with " + LogTag.SCM_VER + " tag",
				"author");
		Mockito.doReturn(commit).when(vcs).getHeadCommit(TEST_BRANCH);
		bs = new BranchStructure(vcs, TEST_BRANCH);
		assertFalse(bs.getHasFeatures());
	}
}
