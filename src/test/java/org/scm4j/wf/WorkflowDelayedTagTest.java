package org.scm4j.wf;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.branch.ReleaseBranchStatus;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.DelayedTagsFile;
import org.scm4j.wf.conf.Option;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
		assertTrue(isPreHeadCommitTaggedWithVersion(compUBL));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTillDb));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTill));
		
		// check Dealyed Tags file
		DelayedTagsFile cf = new DelayedTagsFile();
		assertTrue(cf.getContent().isEmpty());
	}
	
	private boolean isPreHeadCommitTaggedWithVersion(Component comp) {
		ReleaseBranch rb = new ReleaseBranch(comp);
		List<VCSTag> tags = comp.getVCS().getTagsOnRevision(comp.getVCS().getCommitsRange(rb.getName(), null, WalkDirection.DESC, 2).get(1).getRevision());
		for (VCSTag tag : tags) {
			if (tag.getTagName().equals(rb.getCurrentVersion().toPreviousPatch().toReleaseString())) {
				return true;
			}
		}
		return false;

	}
}
