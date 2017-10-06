package org.scm4j.releaser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.Option;
import org.scm4j.releaser.conf.Options;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class WorkflowDelayedTagTest extends WorkflowTestBase {

	
	private final SCMReleaser releaser = new SCMReleaser();
	private final DelayedTagsFile cf = new DelayedTagsFile();
	
	@Before
	@After
	public void setUpTearDown() {
		cf.delete();
	}

	@Test
	public void testBuildWithDelayedTag() throws IOException {
		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(getProgress(action));
		
		// build all
		action = releaser.getActionTree(compUnTill);
		action.execute(getProgress(action));
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), new ReleaseBranch(compUnTillDb, env.getUnTillDbVer()).getName(), "patch feature merged");
		Options.setOptions(Collections.singletonList(Option.DELAYED_TAG));
		
		// build all patches
		action = releaser.getActionTree(compUnTill.clone(env.getUnTillVer().toReleaseZeroPatch()));
		action.execute(getProgress(action));
		
		// build??? all patches
		action = releaser.getActionTree(compUnTill.clone(env.getUnTillVer().toReleaseZeroPatch()));
		action.execute(getProgress(action));
		
		// check no new tags
		assertTrue(env.getUblVCS().getTags().size() == 1);
		assertTrue(env.getUnTillDbVCS().getTags().size() == 1);
		assertTrue(env.getUnTillVCS().getTags().size() == 1);
		
		// check Delayed Tags file
		
		assertNotNull(cf.getRevisitonByUrl(compUnTillDb.getVcsRepository().getUrl()));
		assertNotNull(cf.getRevisitonByUrl(compUnTill.getVcsRepository().getUrl()));
		assertNotNull(cf.getRevisitonByUrl(compUBL.getVcsRepository().getUrl()));
		
		assertEquals(BuildStatus.DONE, new Build(compUnTillDb.clone(env.getUnTillDbVer().toReleaseZeroPatch())).getStatus());
		assertEquals(BuildStatus.DONE, new Build(compUBL.clone(env.getUblVer().toReleaseZeroPatch())).getStatus());
		assertEquals(BuildStatus.DONE, new Build(compUnTill.clone(env.getUnTillVer().toReleaseZeroPatch())).getStatus());
	}
	
	@Test
	public void testTagDelayed() {
		Options.setOptions(Collections.singletonList(Option.DELAYED_TAG));
		
		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		action.execute(getProgress(action));
		
		// build all
		action = releaser.getActionTree(compUnTill);
		action.execute(getProgress(action));
		
		// create delayed tags
		action = releaser.getTagActionTree(compUnTill);
		action.execute(getProgress(action));
		
		// check tags
		assertTrue(isPreHeadCommitTaggedWithVersion(compUBL));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTillDb));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTill));
		
		// check Dealyed Tags file
		
		assertTrue(cf.getContent().isEmpty());
	}
	
	private boolean isPreHeadCommitTaggedWithVersion(Component comp) {
		ReleaseBranch rb = new ReleaseBranch(comp);
		List<VCSTag> tags = comp.getVCS().getTagsOnRevision(comp.getVCS().getCommitsRange(rb.getName(), null, WalkDirection.DESC, 2).get(1).getRevision());
		for (VCSTag tag : tags) {
			if (tag.getTagName().equals(rb.getVersion().toPreviousPatch().toReleaseString())) {
				return true;
			}
		}
		return false;

	}
}


