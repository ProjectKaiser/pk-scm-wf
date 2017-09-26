package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.Option;
import org.scm4j.releaser.conf.Options;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WorkflowDelayedTagTest extends WorkflowTestBase {

	private IProgress nullProgress = new NullProgress();

	@Test
	public void testBuildWithDelayedTag() throws IOException {
		env.generateFeatureCommit(env.getUnTillDbVCS(), dbUnTillDb.getName(), "feature added");
		env.generateFeatureCommit(env.getUnTillVCS(), dbUnTill.getName(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), dbUBL.getName(), "feature added");
		Options.setOptions(Collections.singletonList(Option.DELAYED_TAG));
		SCMReleaser releaser = new SCMReleaser();
		
		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(nullProgress);
		
		// build all
		action = releaser.getActionTree(compUnTill);
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
//		assertEquals(ReleaseBranchStatus.ACTUAL, new ReleaseBranch(compUnTillDb).getStatus());
//		assertEquals(ReleaseBranchStatus.ACTUAL, new ReleaseBranch(compUBL).getStatus());
//		assertEquals(ReleaseBranchStatus.ACTUAL, new ReleaseBranch(compUnTill).getStatus());
	}
	
	@Test
	public void testTagDelayed() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), dbUnTillDb.getName(), "feature added");
		env.generateFeatureCommit(env.getUnTillVCS(), dbUnTill.getName(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), dbUBL.getName(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		Options.setOptions(Collections.singletonList(Option.DELAYED_TAG));
		
		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(nullProgress);
		
		// build all
		action = releaser.getActionTree(compUnTill);
		action.execute(nullProgress);
		
		// create delayed tags
		action = releaser.getActionTree(compUnTill);
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
//		ReleaseBranch rb = new ReleaseBranch(comp);
//		List<VCSTag> tags = comp.getVCS().getTagsOnRevision(comp.getVCS().getCommitsRange(rb.getName(), null, WalkDirection.DESC, 2).get(1).getRevision());
//		for (VCSTag tag : tags) {
//			if (tag.getTagName().equals(rb.getVersion().toPreviousPatch().toReleaseString())) {
//				return true;
//			}
//		}
		return false;

	}
}
