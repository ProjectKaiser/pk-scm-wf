package org.scm4j.releaser.scmactions;

import org.junit.Test;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.actions.ActionSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;


public class SCMActionReleaseTest extends WorkflowTestBase {
	
	@Test
	public void testUnsupportedBuildStatus() throws Exception {
		CachedStatuses cache = new CachedStatuses();
		ExtendedStatus node = new ExtendedStatus(env.getUnTillVer(), BuildStatus.ERROR, new LinkedHashMap<>(), compUnTill, repoUnTill);
		cache.put(repoUnTill.getUrl(), node);
		try {
			new SCMActionRelease(compUnTill, new ArrayList<>(), cache, repoFactory, ActionSet.FULL, false, repoUnTill);
			fail();
		} catch (IllegalArgumentException e) {

		}
	}
	
	@Test
	public void coverToString() {
		CachedStatuses cache = new CachedStatuses();
		ExtendedStatus node = new ExtendedStatus(env.getUnTillVer(), BuildStatus.DONE, new LinkedHashMap<>(), compUnTill, repoUnTill);
		cache.put(repoUnTill.getUrl(), node);
		new SCMActionRelease(compUnTill, new ArrayList<>(), cache, repoFactory, ActionSet.FULL, false, repoUnTill).toString();
	}
}
