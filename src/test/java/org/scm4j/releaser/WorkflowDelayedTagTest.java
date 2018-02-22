package org.scm4j.releaser;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranchCurrent;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.branch.ReleaseBranchPatch;
import org.scm4j.releaser.conf.*;
import org.scm4j.releaser.exceptions.EDelayingDelayed;
import org.scm4j.releaser.exceptions.ENoDelayedTags;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
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
		assertActionDoesBuildAllDelayedTag(action);

		// check no new tags
		Assert.assertEquals(2, env.getUblVCS().getTags().size());
		Assert.assertEquals(2, env.getUnTillDbVCS().getTags().size());
		Assert.assertEquals(1, env.getUnTillVCS().getTags().size());
		
		// check Delayed Tags file
		assertNull(dtf.getDelayedTagByUrl(repoUnTillDb.getUrl()));
		assertNotNull(dtf.getDelayedTagByUrl(repoUnTill.getUrl()));
		assertNull(dtf.getDelayedTagByUrl(repoUBL.getUrl()));

		// check Delayed Tags are used
		action = execAndGetActionBuild(compUnTillVersioned);
		assertActionDoesNothing(action);
	}
	
	@Test
	public void testDelayedTagOnMinor() throws Exception {
		fork(compUnTill);
		IAction action = execAndGetActionBuildDelayedTag(compUnTill);
		assertActionDoesBuildAllDelayedTag(action);

		// check root component tag is delayed
		assertTrue(env.getUnTillVCS().getTags().isEmpty());
		assertTrue(env.getUnTillDbVCS().getTags().size() == 1);
		assertTrue(env.getUblVCS().getTags().size() == 1);

		// check component with delayed tag is considered as tagged (DONE) on build
		action = execAndGetActionBuild(compUnTill);
		assertActionDoesNothing(action);

		// check Delayed Tags file
		assertNull(dtf.getDelayedTagByUrl(repoUnTillDb.getUrl()));
		assertNotNull(dtf.getDelayedTagByUrl(repoUnTill.getUrl()));
		assertNull(dtf.getDelayedTagByUrl(repoUBL.getUrl()));

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
	public void testTagFileUnexpectedlyDeleted() throws Exception {
		// build all, root tag delayed
		fork(compUnTill);
		execAndGetActionBuildDelayedTag(compUnTill);

		// simulate delayed tags file is deleted right before action execution. Expecting no exceptions
		try {
			execAndGetActionTag(compUnTill, () -> assertTrue(dtf.delete()));
			fail();
		} catch (ENoDelayedTags e) {

		}
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
			Map<String, DelayedTag> content = dtf.getContent();
			for (Map.Entry<String, DelayedTag> entry : content.entrySet()) {
				if (repoUnTill.getUrl().equals(entry.getKey())) {
					DelayedTag dt = entry.getValue();
					TagDesc tagDesc = Utils.getTagDesc(dt.getVersion().toString());
					String branchName = Utils.getReleaseBranchName(repoUnTill, dt.getVersion());
					env.getUnTillVCS().createTag(branchName, tagDesc.getName(), tagDesc.getMessage(), dt.getRevision());
				}
			}
			try {
				Thread.sleep(1000); // TODO: test fails without sleep
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
	public void testExceptionIfNoDelayedTags() {
		try {
			execAndGetActionTag(compUnTill, null);
			fail();
		} catch (ENoDelayedTags e) {

		}

		// check no tags
		assertTrue(env.getUnTillVCS().getTags().isEmpty());
		assertTrue(env.getUnTillDbVCS().getTags().isEmpty());
		assertTrue(env.getUblVCS().getTags().isEmpty());
	}
	
	@Test
	public void testTagExistsOnGetActionTree() throws Exception {
		fork(compUnTillDb);
		IAction action = execAndGetActionBuildDelayedTag(compUnTillDb);
		assertActionDoesBuildDelayedTag(action, compUnTillDb);

		DelayedTag delayedTag = dtf.getDelayedTagByUrl(repoUnTillDb.getUrl());
		String branchName = Utils.getReleaseBranchName(repoUnTillDb, delayedTag.getVersion());
		env.getUnTillDbVCS().createTag(branchName, "other-tag", "other tag message", delayedTag.getRevision());
		
		// simulate tag exists
		TagDesc tagDesc = Utils.getTagDesc(delayedTag.getVersion().toString());
		env.getUnTillDbVCS().createTag(branchName, tagDesc.getName(), tagDesc.getMessage(), delayedTag.getRevision());

		Thread.sleep(1000); // TODO: test fails without sleep

		// check version tag is detected -> tagging skipped
		action = execAndGetActionTag(compUnTillDb, null);
		assertActionDoesTag(action, compUnTillDb);

		// check no new tags
		assertTrue(env.getUnTillDbVCS().getTags().size() == 2);
	}

	@Test
	public void testMDepTagDelayed() {
		fork(compUnTillDb);
		IAction action = execAndGetActionBuildDelayedTag(compUnTillDb);
		assertActionDoesBuildDelayedTag(action, compUnTillDb);

		// fork unTill. All should be forked except of unTillDb
		action = execAndGetActionFork(compUnTill);
		checkUnTillForked(1);
		assertActionDoesFork(action, compUnTill, compUBL);
		assertActionDoesNothing(action, compUnTillDb);

		// build unTill. All should be built except of unTillDb
		action = execAndGetActionBuild(compUnTill);
		checkUnTillBuilt(1);
		assertActionDoesBuild(action, compUnTill, BuildStatus.BUILD_MDEPS);
		assertActionDoesBuild(action, compUBL, BuildStatus.BUILD);
		assertActionDoesNothing(action, compUnTillDb);
		
		// check nothing happens on next fork
		action = execAndGetActionFork(compUnTill);
		assertActionDoesNothing(action, compUnTill, compUnTillDb, compUBL);

		// set tag on unTillDb
		assertTrue(env.getUnTillDbVCS().getTags().isEmpty());
		action = execAndGetActionTag(compUnTillDb, null);
		assertActionDoesTag(action, compUnTillDb);
		assertFalse(env.getUnTillDbVCS().getTags().isEmpty());

		// check nothing happens on next fork
		action = execAndGetActionFork(compUnTill);
		assertActionDoesNothing(action, compUnTill, compUnTillDb, compUBL);
	}

	@Test
	public void testDelayingDelayed() {
		fork(compUnTillDb);
		IAction action = execAndGetActionBuildDelayedTag(compUnTillDb);
		assertActionDoesBuildDelayedTag(action, compUnTillDb);

		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), crb.getName(), "feature merged");

		// try to build next untillDb patch with delayed tag
		Component compUnTillDbPatch = new Component(UNTILLDB + ":" + env.getUnTillDbVer().toRelease());
		try {
			execAndGetActionBuildDelayedTag(compUnTillDbPatch);
			fail();
		} catch (EDelayingDelayed e) {
			assertEquals(repoUnTillDb.getUrl(), e.getUrl());
		}
	}
	
	@Test
	public void testDelayedTagVersionUsageIfTrunkBumped() {
		fork(compUnTillDb);
		IAction action = execAndGetActionBuildDelayedTag(compUnTillDb);
		assertActionDoesBuildDelayedTag(action, compUnTillDb);

		new DevelopBranch(compUnTillDb, repoUnTillDb).getVersion();
		
		// simulate version is reaised already in trunk (e.g. built manually)
		IVCS vcs = repoUnTillDb.getVCS();
		vcs.setFileContent(repoUnTillDb.getDevelopBranch(), Utils.VER_FILE_NAME,
				new Version(vcs.getFileContent(repoUnTillDb.getDevelopBranch(), Utils.VER_FILE_NAME, null)).toNextMinor().toString(),
				"minor bumped");
		
		// tag delayed
		action = execAndGetActionTag(compUnTillDb, null);
		assertActionDoesTag(action, compUnTillDb);
		
		ReleaseBranchPatch patchBranch = ReleaseBranchFactory.getReleaseBranchPatch(env.getUnTillDbVer(), repoUnTillDb);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch(), patchBranch.getVersion());
	}

	@Test
	public void testDelayedTagVersionUsageIfPatchBumped() {
		fork(compUnTillDb);
		IAction action = execAndGetActionBuildDelayedTag(compUnTillDb);
		assertActionDoesBuildDelayedTag(action, compUnTillDb);

		new DevelopBranch(compUnTillDb, repoUnTillDb).getVersion();

		// simulate version is reaised already in release branch (e.g. built manually)
		IVCS vcs = repoUnTillDb.getVCS();
		ReleaseBranchPatch patchBranch = ReleaseBranchFactory.getReleaseBranchPatch(env.getUnTillDbVer(), repoUnTillDb);
		VCSCommit headCommit = vcs.setFileContent(patchBranch.getName(), Utils.VER_FILE_NAME,
				new Version(vcs.getFileContent(patchBranch.getName(), Utils.VER_FILE_NAME, null)).toNextPatch().toNextPatch().toString(),
				"patch bumped");

		// tag delayed
		action = execAndGetActionTag(compUnTillDb, null);
		assertActionDoesTag(action, compUnTillDb);

		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toString(), vcs.getTags().get(0).getTagName());

		// check version is not bumped because it is bumped already
		assertEquals(headCommit, env.getUnTillDbVCS().getHeadCommit(patchBranch.getName()));
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


