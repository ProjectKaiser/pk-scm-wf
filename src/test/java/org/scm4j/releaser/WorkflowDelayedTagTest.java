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
import org.scm4j.releaser.cli.CLICommand;
import org.scm4j.releaser.cli.Option;
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

		// add feature to unTill
		Component compUnTillVersioned = compUnTill.clone(env.getUnTillVer().toReleaseZeroPatch());
		ReleaseBranchPatch rb = ReleaseBranchFactory.getReleaseBranchPatch(compUnTillVersioned.getVersion(), repoUnTillDb);
		env.generateFeatureCommit(env.getUnTillVCS(), rb.getName(), "patch feature merged");
		
		// build all patches, delayed tag
		IAction action = execAndGetActionBuildDelayedTag(compUnTillVersioned);
		assertActionDoesBuildDelayedTag(action, compUnTillVersioned);

		// check root component patch tag is delayed
		Assert.assertEquals(1, env.getUblVCS().getTags().size());
		Assert.assertEquals(1, env.getUnTillDbVCS().getTags().size());
		Assert.assertEquals(1, env.getUnTillVCS().getTags().size());

		// check component with delayed tag is considered as tagged (DONE) on build
		action = execAndGetActionBuild(compUnTillVersioned);
		assertActionDoesNothing(action);

		// check no exceptions on status command with --delayed-tag option
		execAndGetNode(null, CLICommand.STATUS.getCmdLineStr(),
				compUnTillVersioned.getCoords().toString(), Option.DELAYED_TAG.getCmdLineStr());

		// check Delayed Tags file
		DelayedTag delayedTag = dtf.getDelayedTagByUrl(repoUnTill.getUrl());
		assertEquals(env.getUnTillVer().toReleaseZeroPatch().toNextPatch(), delayedTag.getVersion());
		ReleaseBranchPatch patchRB = ReleaseBranchFactory.getReleaseBranchPatch(compUnTillVersioned.getVersion(), repoUnTill);
		VCSCommit commitToTag = env.getUnTillVCS().getHeadCommit(patchRB.getName());
		assertEquals(delayedTag.getRevision(), commitToTag.getRevision());

		// create tag which was delayed
		action = execAndGetActionTag(compUnTill, null);
		assertActionDoesTag(action, compUnTill);

		// check tags
		assertTrue(isPreHeadCommitTaggedWithVersion(repoUBL, env.getUblVer()));
		assertTrue(isPreHeadCommitTaggedWithVersion(repoUnTillDb, env.getUnTillDbVer()));
		assertTrue(isPreHeadCommitTaggedWithVersion(repoUnTill, env.getUnTillVer()));

		// check Dealyed Tags file
		assertTrue(dtf.getContent().isEmpty());
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

		// expect no exceptions on status command with --delayed-tag option
		execAndGetNode(null, CLICommand.STATUS.getCmdLineStr(),
				compUnTill.getCoords().toString(), Option.DELAYED_TAG.getCmdLineStr());

		// check Delayed Tags file
		assertNull(dtf.getDelayedTagByUrl(repoUnTillDb.getUrl()));
		assertNotNull(dtf.getDelayedTagByUrl(repoUnTill.getUrl()));
		assertNull(dtf.getDelayedTagByUrl(repoUBL.getUrl()));

		// check delayed tag
		DelayedTag delayedTag = dtf.getDelayedTagByUrl(repoUnTill.getUrl());
		assertEquals(env.getUnTillVer().toReleaseZeroPatch(), delayedTag.getVersion());
		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUnTill);
		VCSCommit commitToTag = env.getUnTillVCS().getHeadCommit(crb.getName());
		assertEquals(delayedTag.getRevision(), commitToTag.getRevision());

		// create tag which was delayed
		action = execAndGetActionTag(compUnTill, null);
		assertActionDoesTag(action, compUnTill);

		// check tags
		assertTrue(isPreHeadCommitTaggedWithVersion(repoUBL, env.getUblVer()));
		assertTrue(isPreHeadCommitTaggedWithVersion(repoUnTillDb, env.getUnTillDbVer()));
		assertTrue(isPreHeadCommitTaggedWithVersion(repoUnTill, env.getUnTillVer()));

		// check Dealyed Tags file
		assertTrue(dtf.getContent().isEmpty());
	}
	
	@Test
	public void testTagFileUnexpectedlyDeleted() throws Exception {
		// build all, root tag delayed
		fork(compUnTill);
		execAndGetActionBuildDelayedTag(compUnTill);

		// simulate delayed tags file is deleted right before action execution.
		try {
			execAndGetActionTag(compUnTill, () -> assertTrue(dtf.delete()));
			fail();
		} catch (ENoDelayedTags e) {

		}

		// check no tags
		assertTrue(env.getUnTillVCS().getTags().isEmpty());
		assertTrue(env.getUnTillDbVCS().getTags().size() == 1);
		assertTrue(env.getUblVCS().getTags().size() == 1);
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
		assertTrue(isPreHeadCommitTaggedWithVersion(repoUBL, env.getUblVer()));
		assertTrue(isPreHeadCommitTaggedWithVersion(repoUnTillDb, env.getUnTillDbVer()));
		assertTrue(isPreHeadCommitTaggedWithVersion(repoUnTill, env.getUnTillVer()));

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
		forkAndBuild(compUnTillDb);

		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), crb.getName(), "feature merged");

		Component compUnTillDbPatch = compUnTillDb.clone(env.getUnTillDbVer().toReleaseZeroPatch());

		IAction action = execAndGetActionBuildDelayedTag(compUnTillDbPatch);
		assertActionDoesBuildDelayedTag(action, compUnTillDbPatch);

		env.generateFeatureCommit(env.getUnTillDbVCS(), crb.getName(), "feature merged");

		// try to build next untillDb patch with delayed tag
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
		vcs.setFileContent(repoUnTillDb.getDevelopBranch(), Constants.VER_FILE_NAME,
				new Version(vcs.getFileContent(repoUnTillDb.getDevelopBranch(), Constants.VER_FILE_NAME, null)).toNextMinor().toString(),
				"minor bumped");
		
		// tag delayed
		action = execAndGetActionTag(compUnTillDb, null);
		assertActionDoesTag(action, compUnTillDb);

		// check right version is used in right release branch
		ReleaseBranchPatch patchBranch = ReleaseBranchFactory.getReleaseBranchPatch(env.getUnTillDbVer(), repoUnTillDb);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), patchBranch.getVersion());

		// check tags
		assertTrue(isPreHeadCommitTaggedWithVersion(repoUnTillDb, env.getUnTillDbVer()));

		// check Dealyed Tags file
		assertTrue(dtf.getContent().isEmpty());
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
		VCSCommit patchBranchHeadCommit = vcs.setFileContent(patchBranch.getName(), Constants.VER_FILE_NAME,
				new Version(vcs.getFileContent(patchBranch.getName(), Constants.VER_FILE_NAME, null)).toNextPatch().toNextPatch().toString(),
				"patch bumped");

		// tag delayed
		action = execAndGetActionTag(compUnTillDb, null);
		assertActionDoesTag(action, compUnTillDb);

		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toString(), vcs.getTags().get(0).getTagName());

		// check version is not bumped because it is bumped already
		assertEquals(patchBranchHeadCommit, env.getUnTillDbVCS().getHeadCommit(patchBranch.getName()));
	}
	
	@Test
	public void testDealyedTagVersionUsageOnDifferentCRB() {
		fork(compUnTillDb);
		IAction action = execAndGetActionBuildDelayedTag(compUnTillDb);
		assertActionDoesBuildDelayedTag(action, compUnTillDb);
		
		// make next build
		env.generateFeatureCommit(env.getUnTillDbVCS(), repoUnTillDb.getDevelopBranch(), "feature added to dev branch");
		fork(compUnTillDb, 2);
		execAndGetActionBuild(compUnTillDb);
		
		// generate next feature to make CRB differ
		env.generateFeatureCommit(env.getUnTillDbVCS(), repoUnTillDb.getDevelopBranch(), "feature added to dev branch");
		
		// make a tag for version which is not current CRB
		action = execAndGetActionTag(compUnTillDb, null);
		assertActionDoesTag(action, compUnTillDb);
		
		// ensure the version for delayed tag is used
		ReleaseBranchPatch patch = ReleaseBranchFactory.getReleaseBranchPatch(env.getUnTillDbVer(), repoUnTillDb);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), patch.getVersion());

		// check tags
		assertTrue(isPreHeadCommitTaggedWithVersion(repoUnTillDb, env.getUnTillDbVer()));

		// check Delayed Tags file
		assertTrue(dtf.getContent().isEmpty());
	}

	private boolean isPreHeadCommitTaggedWithVersion(VCSRepository repo, Version forVersion) {
		ReleaseBranchPatch rb = ReleaseBranchFactory.getReleaseBranchPatch(forVersion, repo);
		List<VCSTag> tags = repo.getVCS().getTagsOnRevision(repo.getVCS().getCommitsRange(rb.getName(), null, WalkDirection.DESC, 2).get(1).getRevision());
		for (VCSTag tag : tags) {
			if (tag.getTagName().equals(rb.getVersion().toPreviousPatch().toReleaseString())) {
				return true;
			}
		}
		return false;
	}
}
