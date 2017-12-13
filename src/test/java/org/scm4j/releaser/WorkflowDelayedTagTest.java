package org.scm4j.releaser;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.MDepsSource;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

public class WorkflowDelayedTagTest extends WorkflowTestBase {

	private final DelayedTagsFile dtf = new DelayedTagsFile();
	
	@Before
	@After
	public void setUpTearDown() {
		dtf.delete();
	}

	@Test
	public void testDelayedTagOnPatch() throws Exception {
		// build all
		IAction action = getActionTreeBuild(compUnTill);
		assertIsGoingToForkAndBuildAll(action);
		execAction(action);

		// add feature to unTillDb release/2.59
		Component compUnTillDbVersioned = compUnTillDb.clone(env.getUnTillDbVer());
		MDepsSource mDepsSource = new MDepsSource(compUnTillDbVersioned);
		env.generateFeatureCommit(env.getUnTillDbVCS(),  mDepsSource.getRbName(), "patch feature merged");
		
		// build all patches, delayed tag
		Component compUnTillVersioned = compUnTill.clone(env.getUnTillVer().toReleaseZeroPatch());
		action = getActionTreeDelayedTag(compUnTillVersioned); //actionBuilder.getActionTreeDelayedTag(compUnTillVersioned);
		assertIsGoingToBuildAll(action);
		execAction(action);
		
		// check no new tags
		assertTrue(env.getUblVCS().getTags().size() == 1);
		assertTrue(env.getUnTillDbVCS().getTags().size() == 1);
		assertTrue(env.getUnTillVCS().getTags().size() == 1);
		
		// check Delayed Tags file
		assertNotNull(dtf.getRevisitonByUrl(compUnTillDb.getVcsRepository().getUrl()));
		assertNotNull(dtf.getRevisitonByUrl(compUnTill.getVcsRepository().getUrl()));
		assertNotNull(dtf.getRevisitonByUrl(compUBL.getVcsRepository().getUrl()));

		// check Delayed Tags are used
		action = getActionTreeBuild(compUnTillVersioned);
		assertIsGoingToDoNothing(action);
	}
	
	@Test
	public void testDelayedTag() throws Exception {
		IAction action = getActionTreeDelayedTag(compUnTill);
		execAction(action);

		// check no tags
		assertTrue(env.getUnTillVCS().getTags().isEmpty());
		assertTrue(env.getUnTillDbVCS().getTags().isEmpty());
		assertTrue(env.getUblVCS().getTags().isEmpty());

		// check Delayed Tags file
		assertNotNull(dtf.getRevisitonByUrl(compUnTillDb.getVcsRepository().getUrl()));
		assertNotNull(dtf.getRevisitonByUrl(compUnTill.getVcsRepository().getUrl()));
		assertNotNull(dtf.getRevisitonByUrl(compUBL.getVcsRepository().getUrl()));

		// tag all
		action = getActionTreeTag(compUnTill);
		assertIsGoingToTagAll(action);
		execAction(action);

		// check tags
		assertTrue(isPreHeadCommitTaggedWithVersion(compUBL));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTillDb));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTill));

		// check Dealyed Tags file
		assertTrue(dtf.getContent().isEmpty());
	}

	@Test
	public void testTagFileDeleted() throws Exception {
		IAction action = getActionTreeDelayedTag(compUnTill);
		execAction(action);

		// simulate delayed tags file is deleted right after action create
		action = getActionTreeTag(compUnTill);
		assertIsGoingToTagAll(action);
		dtf.delete();
		execAction(action);

		// check no tags
		assertTrue(env.getUnTillVCS().getTags().isEmpty());
		assertTrue(env.getUnTillDbVCS().getTags().isEmpty());
		assertTrue(env.getUblVCS().getTags().isEmpty());
	}

	@Test
	public void testTagExistsOnExecute() throws Exception {
		// build all
		IAction action = getActionTreeDelayedTag(compUnTill);
		execAction(action);

		// all is going to tag
		action = getActionTreeTag(compUnTill);
		assertIsGoingToTagAll(action);

		// simulate tag exists already
		MDepsSource mDepsSource = new MDepsSource(compUnTill);
		//WorkingBranch rbUnTill = new WorkingBranch(compUnTill);
		Map<String, String> content = dtf.getContent();
		for (Map.Entry<String, String> entry : content.entrySet()) {
			if (compUnTill.getVcsRepository().getUrl().equals(entry.getKey())) {
				Version delayedTagVersion = new Version(env.getUnTillVCS().getFileContent(mDepsSource.getRbName(), ActionTreeBuilder.VER_FILE_NAME,
						entry.getValue()));
				TagDesc tagDesc = Utils.getTagDesc(delayedTagVersion.toString());
				env.getUnTillVCS().createTag(mDepsSource.getRbName(), tagDesc.getName(), tagDesc.getMessage(), entry.getValue());
			}
		}

		Thread.sleep(1000); // FIXME: test fails without sleep
		// tagging should be skipped with no exceptions
		execAction(action);

		// check tags
		assertTrue(isPreHeadCommitTaggedWithVersion(compUBL));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTillDb));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTill));

		// check Dealyed Tags file
		assertTrue(dtf.getContent().isEmpty());
	}
	
	@Test
	public void testDoNothingIfNoDelayedTags() {
		IAction action = getActionTreeTag(compUnTillDb);
		assertIsGoingToTag(action, compUnTillDb);
		execAction(action);

		// check no tags
		assertTrue(env.getUnTillVCS().getTags().isEmpty());
		assertTrue(env.getUnTillDbVCS().getTags().isEmpty());
		assertTrue(env.getUblVCS().getTags().isEmpty());
	}
	
	@Test
	public void testTagExistsOnGetActionTree() throws Exception {
		// build all
		IAction action = getActionTreeDelayedTag(compUnTillDb);
		assertIsGoingToForkAndBuild(action, compUnTillDb);
		execAction(action);

		String revisionToTag = dtf.getRevisitonByUrl(compUnTillDb.getVcsRepository().getUrl());
		MDepsSource mDepsSource = new MDepsSource(compUnTillDb);
		env.getUnTillDbVCS().createTag(mDepsSource.getRbName(), "other-tag", "other tag message", revisionToTag);
		
		// simulate tag exists
		Version delayedTagVersion = new Version(env.getUnTillDbVCS().getFileContent(mDepsSource.getRbName(), ActionTreeBuilder.VER_FILE_NAME,
				revisionToTag));
		TagDesc tagDesc = Utils.getTagDesc(delayedTagVersion.toString());
		env.getUnTillDbVCS().createTag(mDepsSource.getRbName(), tagDesc.getName(), tagDesc.getMessage(), revisionToTag);

		Thread.sleep(1000); // FIXME: test fails without sleep

		// check version tag is detected -> tagging skipped
		action = getActionTreeTag(compUnTillDb);
		assertIsGoingToTag(action, compUnTillDb);
		execAction(action);

		// check no new tags
		assertTrue(env.getUnTillDbVCS().getTags().size() == 2);

	}
	
	private boolean isPreHeadCommitTaggedWithVersion(Component comp) {
		MDepsSource mDepsSource = new MDepsSource(comp);
		List<VCSTag> tags = comp.getVCS().getTagsOnRevision(comp.getVCS().getCommitsRange(mDepsSource.getRbName(), null, WalkDirection.DESC, 2).get(1).getRevision());
		for (VCSTag tag : tags) {
			if (tag.getTagName().equals(mDepsSource.getRbVersion().toPreviousPatch().toReleaseString())) {
				return true;
			}
		}
		return false;
	}
}


