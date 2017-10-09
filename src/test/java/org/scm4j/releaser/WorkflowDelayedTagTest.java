package org.scm4j.releaser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.*;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class WorkflowDelayedTagTest extends WorkflowTestBase {

	private final SCMReleaser releaser = new SCMReleaser();
	private final DelayedTagsFile dtf = new DelayedTagsFile();
	
	@Before
	@After
	public void setUpTearDown() {
		dtf.delete();
	}

	@Test
	public void testBuildWithDelayedTag() throws IOException {
		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		assertIsGoingToForkAll(action);
		action.execute(getProgress(action));
		
		// build all
		action = releaser.getActionTree(compUnTill);
		assertIsGoingToBuildAll(action);
		action.execute(getProgress(action));
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), new ReleaseBranch(compUnTillDb, env.getUnTillDbVer()).getName(), "patch feature merged");
		Options.setOptions(Collections.singletonList(Option.DELAYED_TAG));
		
		// build all patches
		action = releaser.getActionTree(compUnTill.clone(env.getUnTillVer().toReleaseZeroPatch()));
		assertIsGoingToBuildAll(action);
		action.execute(getProgress(action));
		
		// check no new tags
		assertTrue(env.getUblVCS().getTags().size() == 1);
		assertTrue(env.getUnTillDbVCS().getTags().size() == 1);
		assertTrue(env.getUnTillVCS().getTags().size() == 1);
		
		// check Delayed Tags file
		assertNotNull(dtf.getRevisitonByUrl(compUnTillDb.getVcsRepository().getUrl()));
		assertNotNull(dtf.getRevisitonByUrl(compUnTill.getVcsRepository().getUrl()));
		assertNotNull(dtf.getRevisitonByUrl(compUBL.getVcsRepository().getUrl()));

		// check Delayed Tags are used
		action = releaser.getActionTree(compUnTill.clone(env.getUnTillVer().toReleaseZeroPatch()));
		assertThat(action, allOf(
				instanceOf(ActionNone.class),
				hasProperty("mbs", equalTo(BuildStatus.DONE))), compUnTill, compUnTillDb, compUBL);
	}
	
	@Test
	public void testTagDelayed() throws IOException {
		Options.setOptions(Collections.singletonList(Option.DELAYED_TAG));
		
		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		assertIsGoingToForkAll(action);
		action.execute(getProgress(action));
		
		// build all
		action = releaser.getActionTree(compUnTill);
		assertIsGoingToBuildAll(action);
		action.execute(getProgress(action));

		// check no tags
		assertTrue(env.getUnTillVCS().getTags().isEmpty());
		assertTrue(env.getUnTillDbVCS().getTags().isEmpty());
		assertTrue(env.getUblVCS().getTags().isEmpty());
		
		// simulate delayed tags are deleted right after action create
		action = releaser.getTagActionTree(compUnTill);
		assertIsGoingToTagAll(action);
		Map<String, String> content = dtf.getContent();
		dtf.delete();
		action.execute(getProgress(action));

		// expect no exceptions and no tags
		assertTrue(env.getUnTillVCS().getTags().isEmpty());
		assertTrue(env.getUnTillDbVCS().getTags().isEmpty());
		assertTrue(env.getUblVCS().getTags().isEmpty());
		for (Map.Entry<String, String> entry : content.entrySet()) {
			dtf.writeUrlRevision(entry.getKey(), entry.getValue());
		}

		// create delayed tags
		action.execute(getProgress(action));

		// check tags
		assertTrue(isPreHeadCommitTaggedWithVersion(compUBL));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTillDb));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTill));
		
		// check Dealyed Tags file
		assertTrue(dtf.getContent().isEmpty());
	}

	@Test
	public void testTagExistsOnExecute() throws InterruptedException {
		Options.setOptions(Collections.singletonList(Option.DELAYED_TAG));

		// fork all
		IAction action = releaser.getActionTree(compUnTill);
		assertIsGoingToForkAll(action);
		action.execute(getProgress(action));

		// build all
		action = releaser.getActionTree(compUnTill);
		assertIsGoingToBuildAll(action);
		action.execute(getProgress(action));

		// all is going to tag
		action = releaser.getTagActionTree(compUnTill);
		assertIsGoingToTagAll(action);

		// simulate tag exists already
		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill);
		Map<String, String> content = dtf.getContent();
		for (Map.Entry<String, String> entry : content.entrySet()) {
			if (compUnTill.getVcsRepository().getUrl().equals(entry.getKey())) {
				Version delayedTagVersion = new Version(env.getUnTillVCS().getFileContent(rbUnTill.getName(), SCMReleaser.VER_FILE_NAME,
						entry.getValue()));
				TagDesc tagDesc = SCMReleaser.getTagDesc(delayedTagVersion.toString());
				env.getUnTillVCS().createTag(rbUnTill.getName(), tagDesc.getName(), tagDesc.getMessage(), entry.getValue());
			}
		}

		Thread.sleep(1000); // FIXME: test fails without sleep
		action.execute(getProgress(action));

		// check tags
		assertTrue(isPreHeadCommitTaggedWithVersion(compUBL));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTillDb));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTill));

		// check Dealyed Tags file
		assertTrue(dtf.getContent().isEmpty());

	}
	
	@Test
	public void testActionNoneIfNoDelayedTag() {
		IAction action = releaser.getTagActionTree(compUnTillDb);
		assertTrue(action instanceof ActionNone);
	}
	
	@Test
	public void testTagExistsOnGetActionTree() {
		Options.setOptions(Collections.singletonList(Option.DELAYED_TAG));

		// fork all
		IAction action = releaser.getActionTree(compUnTillDb);
		assertIsGoingToFork(action, compUnTillDb);
		action.execute(getProgress(action));

		// build all
		action = releaser.getActionTree(compUnTillDb);
		assertIsGoingToBuild(action, compUnTillDb);
		action.execute(getProgress(action));
		
		String revisionToTag = dtf.getRevisitonByUrl(compUnTillDb.getVcsRepository().getUrl());
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		env.getUnTillDbVCS().createTag(rbUnTillDb.getName(), "other-tag", "other tag message", revisionToTag);
		
		// check other tag is bypassed
		action = releaser.getTagActionTree(compUnTillDb);
		assertIsGoingToTag(action, compUnTillDb);
		
		// simulate tag exists
		Version delayedTagVersion = new Version(env.getUnTillDbVCS().getFileContent(rbUnTillDb.getName(), SCMReleaser.VER_FILE_NAME,
				revisionToTag));
		TagDesc tagDesc = SCMReleaser.getTagDesc(delayedTagVersion.toString());
		env.getUnTillDbVCS().createTag(rbUnTillDb.getName(), tagDesc.getName(), tagDesc.getMessage(), revisionToTag);
		
		// check other tag is bypassed, but version tag is detected -> tagging skipped
		action = releaser.getTagActionTree(compUnTillDb);
		assertTrue(action instanceof ActionNone);
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


