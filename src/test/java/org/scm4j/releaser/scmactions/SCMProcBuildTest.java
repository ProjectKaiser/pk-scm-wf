package org.scm4j.releaser.scmactions;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.exceptions.EReleaserException;

public class SCMProcBuildTest extends WorkflowTestBase {
	
	@Test
	public void testNoReleaseBranch() {
		ReleaseBranch rb = new ReleaseBranch(compUBL);
		ISCMProc proc = new SCMProcBuild(rb);
		try {
			proc.execute(new ProgressConsole());
			fail();
		} catch (EReleaserException e) {
			
		}
	}

}
