package org.scm4j.wf;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.branch.ReleaseBranchStatus;
import org.scm4j.wf.conf.DelayedTagsFile;
import org.scm4j.wf.conf.Option;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class WorkflowDelayedTagTest extends WorkflowTestBase {
	
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
		assertEquals(ReleaseBranchStatus.ACTUAL, new ReleaseBranch(compUnTillDb).getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, new ReleaseBranch(compUBL).getStatus());
		assertEquals(ReleaseBranchStatus.ACTUAL, new ReleaseBranch(compUnTill).getStatus());
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
		assertTrue(new ReleaseBranch(compUBL).isPreHeadCommitTaggedWithVersion());
		assertTrue( new ReleaseBranch(compUnTillDb).isPreHeadCommitTaggedWithVersion());
		assertTrue(new ReleaseBranch(compUnTill).isPreHeadCommitTaggedWithVersion());
		
		// check Dealyed Tags file
		DelayedTagsFile cf = new DelayedTagsFile();
		assertTrue(cf.getContent().isEmpty());
	}
}
