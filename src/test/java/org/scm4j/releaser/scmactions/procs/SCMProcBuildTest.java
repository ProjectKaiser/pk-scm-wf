package org.scm4j.releaser.scmactions.procs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.LinkedHashMap;

import org.junit.Test;
import org.mockito.Mockito;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatusTreeNode;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.TestEnvironment;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.exceptions.ENoBuilder;
import org.scm4j.releaser.exceptions.EReleaserException;

public class SCMProcBuildTest extends WorkflowTestBase {
	
	@Test
	public void testNoReleaseBranch() {
		CachedStatuses cache = new CachedStatuses();
		cache.put(compUBL.getUrl(), new ExtendedStatusTreeNode(env.getUblVer(), BuildStatus.BUILD, new LinkedHashMap<>(), compUBL));
		ISCMProc proc = new SCMProcBuild(compUBL, cache);
		try {
			proc.execute(new ProgressConsole());
			fail();
		} catch (EReleaserException e) {
			
		}
	}
	
	@Test
	public void testNoBuilder() throws Exception {
		Component mockedComp = Mockito.spy(new Component(TestEnvironment.PRODUCT_UNTILL));
		VCSRepository mockedRepo = Mockito.spy(mockedComp.getVcsRepository());
		Mockito.when(mockedComp.getVcsRepository()).thenReturn(mockedRepo);
		Mockito.when(mockedRepo.getBuilder()).thenReturn(null);
		
		// avoid "no release branch exception"
		IAction action = new SCMReleaser().getActionTree(mockedComp, ActionSet.FORK_ONLY);
		action.execute(new ProgressConsole());
		CachedStatuses cache = new CachedStatuses();
		cache.put(mockedComp.getUrl(), new ExtendedStatusTreeNode(env.getUnTillVer(), BuildStatus.BUILD, new LinkedHashMap<>(), mockedComp));
		ISCMProc proc = new SCMProcBuild(mockedComp, cache);
		try {
			proc.execute(new ProgressConsole());
			fail();
		} catch (ENoBuilder e) {
			assertEquals(mockedComp, e.getComp());
		}
	}
}
