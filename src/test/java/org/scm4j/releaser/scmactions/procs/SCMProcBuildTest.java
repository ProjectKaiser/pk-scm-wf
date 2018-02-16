package org.scm4j.releaser.scmactions.procs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.LinkedHashMap;

import org.junit.Test;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.exceptions.ENoReleaseBranch;

public class SCMProcBuildTest extends WorkflowTestBase {
	
	@Test
	public void testNoReleaseBranch() {
		CachedStatuses cache = new CachedStatuses();
		cache.put(repoUBL.getUrl(), new ExtendedStatus(env.getUblVer(), BuildStatus.BUILD, new LinkedHashMap<>(), compUBL, repoUBL));
		ISCMProc proc = new SCMProcBuild(compUBL, cache, false, repoUBL);
		try {
			proc.execute(new ProgressConsole());
			fail();
		} catch (ENoReleaseBranch e) {
			assertEquals(Utils.getReleaseBranchName(repoUBL, env.getUblVer()), e.getReleaseBranchName());
		}
	}
}
