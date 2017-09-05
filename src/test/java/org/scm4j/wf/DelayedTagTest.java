package org.scm4j.wf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.ReleaseBranchStatus;
import org.scm4j.wf.conf.DelayedTagsFile;
import org.scm4j.wf.conf.Option;

public class DelayedTagTest extends SCMWorkflowTestBase {
	
	private IProgress nullProgress = new NullProgress();

	@Test
	public void testBuildWithDelayedTag() throws IOException {
		env.generateFeatureCommit(env.getUnTillDbVCS(), dbUnTillDb.getName(), "feature added");
		env.generateFeatureCommit(env.getUnTillVCS(), dbUnTill.getName(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), dbUBL.getName(), "feature added");
		SCMWorkflow wf = new SCMWorkflow(Arrays.asList(Option.DELAYED_TAG));
		
		// fork all
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		
		// build all
		action = wf.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		
		// check no tags
		assertTrue(env.getUblVCS().getTags().isEmpty());
		assertTrue(env.getUnTillDbVCS().getTags().isEmpty());
		assertTrue(env.getUnTillVCS().getTags().isEmpty());
		
		// check Delayed Tags file
		DelayedTagsFile cf = new DelayedTagsFile();
		assertNotNull(cf.getRevisitonByUrl(compUnTillDb.getVcsRepository().getUrl()));
		assertNotNull(cf.getRevisitonByUrl(compUnTill.getVcsRepository().getUrl()));
		assertNotNull(cf.getRevisitonByUrl(compUBL.getVcsRepository().getUrl()));
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUnTillDbFixedVer.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUBLFixedVer.getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, rbUnTillFixedVer.getStatus());
	}
	
	@Test
	public void testTagDelayed() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), dbUnTillDb.getName(), "feature added");
		env.generateFeatureCommit(env.getUnTillVCS(), dbUnTill.getName(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), dbUBL.getName(), "feature added");
		SCMWorkflow wf = new SCMWorkflow(Arrays.asList(Option.DELAYED_TAG));
		
		// fork all
		IAction action = wf.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		
		// build all
		action = wf.getProductionReleaseAction(compUnTill);
		action.execute(nullProgress);
		
		// create delayed tags
		action = wf.getTagReleaseAction(compUnTill);
		action.execute(nullProgress);
		
		// check tags
		assertTrue(rbUBLFixedVer.isPreHeadCommitTaggedWithVersion());
		assertTrue(rbUnTillDbFixedVer.isPreHeadCommitTaggedWithVersion());
		assertTrue(rbUnTillFixedVer.isPreHeadCommitTaggedWithVersion());
		
		// check Dealyed Tags file
		DelayedTagsFile cf = new DelayedTagsFile();
		assertTrue(cf.getContent().isEmpty());
	}
}
