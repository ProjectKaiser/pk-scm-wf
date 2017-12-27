package org.scm4j.releaser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranchCurrent;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.branch.ReleaseBranchPatch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class WorkflowDelayedTagTest extends WorkflowTestBase {

	private final DelayedTagsFile dtf = new DelayedTagsFile();
	
	@Before
	@After
	public void setUpTearDown() {
		dtf.delete();
	}

	@Test
	public void testDelayedTagOnPatch() throws Exception {
		forkAndBuild(compUnTill);

		// add feature to unTillDb release/2.59
		Component compUnTillDbVersioned = compUnTillDb.clone(env.getUnTillDbVer());
		ReleaseBranchPatch rb = ReleaseBranchFactory.getReleaseBranchPatch(compUnTillDbVersioned.getVersion(), repoUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), rb.getName(), "patch feature merged");
		
		// build all patches, delayed tag
		Component compUnTillVersioned = compUnTill.clone(env.getUnTillVer().toReleaseZeroPatch());
		IAction action = execAndGetActionBuildDelayedTag(compUnTillVersioned);
		assertActionDoesBuildAll(action);
		
		// check no new tags
		assertEquals(2, env.getUblVCS().getTags().size());
		assertEquals(2, env.getUnTillDbVCS().getTags().size());
		assertEquals(1, env.getUnTillVCS().getTags().size());
		
		// check Delayed Tags file
		assertNull(dtf.getRevisitonByUrl(repoUnTillDb.getUrl()));
		assertNotNull(dtf.getRevisitonByUrl(repoUnTill.getUrl()));
		assertNull(dtf.getRevisitonByUrl(repoUBL.getUrl()));

		// check Delayed Tags are used
		action = execAndGetActionBuild(compUnTillVersioned);
		assertActionDoesNothing(action);
	}
	
	@Test
	public void testDelayedTag() throws Exception {
		fork(compUnTill);
		execAndGetActionBuildDelayedTag(compUnTill);

		// check root component tag is delayed
		assertTrue(env.getUnTillVCS().getTags().isEmpty());
		assertTrue(env.getUnTillDbVCS().getTags().size() == 1);
		assertTrue(env.getUblVCS().getTags().size() == 1);

		// check component with delayed tag is considered as tagged (DONE) on build
		IAction action = execAndGetActionBuild(compUnTill);
		assertActionDoesNothing(action);

		// check Delayed Tags file
		assertNull(dtf.getRevisitonByUrl(repoUnTillDb.getUrl()));
		assertNotNull(dtf.getRevisitonByUrl(repoUnTill.getUrl()));
		assertNull(dtf.getRevisitonByUrl(repoUBL.getUrl()));

		// create tag which was delayed
		action = execAndGetActionTag(compUnTill, null);
		assertActionDoesTag(action, compUnTill);

		// check tags
		assertTrue(isPreHeadCommitTaggedWithVersion(compUBL));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTillDb));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTill));

		// check Dealyed Tags file
		assertTrue(dtf.getContent().isEmpty());
	}

	@Test
	public void testTagFileDeleted() throws Exception {
		// build all, root tag delayed
		fork(compUnTill);
		execAndGetActionBuildDelayedTag(compUnTill);

		// simulate delayed tags file is deleted right before action execution
		IAction action = execAndGetActionTag(compUnTill, () -> dtf.delete());
		assertActionDoesTag(action, compUnTill);

		// check no tags
		assertTrue(env.getUnTillVCS().getTags().isEmpty());
		assertFalse(env.getUnTillDbVCS().getTags().isEmpty());
		assertFalse(env.getUblVCS().getTags().isEmpty());
	}

	@Test
	public void testTagExistsOnExecute() {
		// build all
		fork(compUnTill);
		execAndGetActionBuildDelayedTag(compUnTill);

		// all is going to tag
		IAction action = execAndGetActionTag(compUnTill, () -> {
			// simulate tag exists already
			// tagging should be skipped with no exceptions
			ReleaseBranchCurrent rb = ReleaseBranchFactory.getCRB(repoUnTill);
			Map<String, String> content = dtf.getContent();
			for (Map.Entry<String, String> entry : content.entrySet()) {
				if (repoUnTill.getUrl().equals(entry.getKey())) {
					Version delayedTagVersion = new Version(env.getUnTillVCS().getFileContent(rb.getName(), Utils.VER_FILE_NAME,
							entry.getValue()));
					TagDesc tagDesc = Utils.getTagDesc(delayedTagVersion.toString());
					env.getUnTillVCS().createTag(rb.getName(), tagDesc.getName(), tagDesc.getMessage(), entry.getValue());
				}
			}
			try {
				Thread.sleep(1000); // FIXME: test fails without sleep
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
		assertActionDoesTag(action, compUnTill);

		// check tags
		assertTrue(isPreHeadCommitTaggedWithVersion(compUBL));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTillDb));
		assertTrue(isPreHeadCommitTaggedWithVersion(compUnTill));

		// check Dealyed Tags file
		assertTrue(dtf.getContent().isEmpty());
	}
	
	@Test
	public void testDoNothingIfNoDelayedTags() {
		IAction action = execAndGetActionTag(compUnTillDb, null);
		assertActionDoesTag(action, compUnTillDb);

		// check no tags
		assertTrue(env.getUnTillVCS().getTags().isEmpty());
		assertTrue(env.getUnTillDbVCS().getTags().isEmpty());
		assertTrue(env.getUblVCS().getTags().isEmpty());
	}
	
	@Test
	public void testTagExistsOnGetActionTree() throws Exception {
		fork(compUnTillDb);
		IAction action = execAndGetActionBuildDelayedTag(compUnTillDb);
		assertActionDoesBuild(action, compUnTillDb);

		String revisionToTag = dtf.getRevisitonByUrl(repoUnTillDb.getUrl());
		ReleaseBranchCurrent rb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		env.getUnTillDbVCS().createTag(rb.getName(), "other-tag", "other tag message", revisionToTag);
		
		// simulate tag exists
		Version delayedTagVersion = new Version(env.getUnTillDbVCS().getFileContent(rb.getName(), Utils.VER_FILE_NAME,
				revisionToTag));
		TagDesc tagDesc = Utils.getTagDesc(delayedTagVersion.toString());
		env.getUnTillDbVCS().createTag(rb.getName(), tagDesc.getName(), tagDesc.getMessage(), revisionToTag);

		Thread.sleep(1000); // FIXME: test fails without sleep

		// check version tag is detected -> tagging skipped
		action = execAndGetActionTag(compUnTillDb, null);
		assertActionDoesTag(action, compUnTillDb);

		// check no new tags
		assertTrue(env.getUnTillDbVCS().getTags().size() == 2);
	}
	
	private boolean isPreHeadCommitTaggedWithVersion(Component comp) {
		VCSRepository repo = repoFactory.getVCSRepository(comp);
		ReleaseBranchCurrent rb = ReleaseBranchFactory.getCRB(repo);
		List<VCSTag> tags = repo.getVCS().getTagsOnRevision(repo.getVCS().getCommitsRange(rb.getName(), null, WalkDirection.DESC, 2).get(1).getRevision());
		for (VCSTag tag : tags) {
			if (tag.getTagName().equals(rb.getVersion().toPreviousPatch().toReleaseString())) {
				return true;
			}
		}
		return false;
	}
}


