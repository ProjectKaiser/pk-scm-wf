package org.scm4j.releaser.scmactions;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.junit.Test;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.actions.ActionSet;


public class SCMActionReleaseTest extends WorkflowTestBase {
	
	@Test
	public void testUnsupportedBuildStatus() throws Exception {
		CachedStatuses cache = new CachedStatuses();
		ExtendedStatus node = new ExtendedStatus(env.getUnTillVer(), BuildStatus.ERROR, new LinkedHashMap<>(), compUnTill);
		cache.put(compUnTill.getUrl(), node);
		try {
			new SCMActionRelease(compUnTill, new ArrayList<>(), cache, ActionSet.FULL, false);
			fail();
		} catch (IllegalArgumentException e) {

		}
	}
	
	@Test
	public void testToString() {
		CachedStatuses cache = new CachedStatuses();
		ExtendedStatus node = new ExtendedStatus(env.getUnTillVer(), BuildStatus.DONE, new LinkedHashMap<>(), compUnTill);
		cache.put(compUnTill.getUrl(), node);
		new SCMActionRelease(compUnTill, new ArrayList<>(), cache, ActionSet.FULL, false).toString();
	}
}
