package org.scm4j.releaser.scmactions.procs;

import static org.junit.Assert.fail;

import java.util.LinkedHashMap;

import org.junit.Test;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.exceptions.EReleaserException;

public class SCMProcBuildTest extends WorkflowTestBase {
	
	@Test
	public void testNoReleaseBranch() {
		CachedStatuses cache = new CachedStatuses();
		cache.put(compUBL.getUrl(), new ExtendedStatus(env.getUblVer(), BuildStatus.BUILD, new LinkedHashMap<>(), compUBL));
		ISCMProc proc = new SCMProcBuild(compUBL, cache, false);
		try {
			proc.execute(new ProgressConsole());
			fail();
		} catch (EReleaserException e) {
			
		}
	}
	
}
