package org.scm4j.releaser;

import org.junit.After;
import org.junit.Assert;
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
		assertNull(dtf.getRevisitonByUrl(repoUnTillDb.getUrl()));
		assertNotNull(dtf.getRevisitonByUrl(repoUnTill.getUrl()));
		assertNull(dtf.getRevisitonByUrl(repoUBL.getUrl()));

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
	public void testTagFileUnexpectedlyDeleted() throws Exception {
		// build all, root tag delayed
		fork(compUnTill);
		execAndGetActionBuildDelayedTag(compUnTill);

		// simulate delayed tags file is deleted right before action execution. Expecting no exceptions
		IAction action = execAndGetActionTag(compUnTill, () -> assertTrue(dtf.delete()));
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
	public void testDoNothingIfNoDelayedTags() {
		IAction action = execAndGetActionTag(compUnTill, null);
		assertActionDoesTag(action, compUnTill);

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

		String revisionToTag = dtf.getRevisitonByUrl(repoUnTillDb.getUrl());
		ReleaseBranchCurrent rb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		env.getUnTillDbVCS().createTag(rb.getName(), "other-tag", "other tag message", revisionToTag);
		
		// simulate tag exists
		Version delayedTagVersion = new Version(env.getUnTillDbVCS().getFileContent(rb.getName(), Utils.VER_FILE_NAME,
				revisionToTag));
		TagDesc tagDesc = Utils.getTagDesc(delayedTagVersion.toString());
		env.getUnTillDbVCS().createTag(rb.getName(), tagDesc.getName(), tagDesc.getMessage(), revisionToTag);

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
	public void testVersionIsBumpedAlreadyOnTag() {
		fork(compUnTillDb);
		IAction action = execAndGetActionBuildDelayedTag(compUnTillDb);
		assertActionDoesBuildDelayedTag(action, compUnTillDb);

		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		VCSCommit headCommit = env.getUnTillDbVCS().setFileContent(crb.getName(), Utils.VER_FILE_NAME,
				crb.getVersion().toNextPatch().toString(), "version bumped");

		// set tag on unTillDb. Version file should not be changed
		action = execAndGetActionTag(compUnTillDb, null);
		assertActionDoesTag(action, compUnTillDb);

		assertEquals(headCommit, env.getUnTillDbVCS().getHeadCommit(crb.getName()));
	}

	@Test
	public void testVersionIsBumpedFewTimesAlreadyOnTag() {
		fork(compUnTillDb);
		IAction action = execAndGetActionBuildDelayedTag(compUnTillDb);
		assertActionDoesBuildDelayedTag(action, compUnTillDb);

		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		VCSCommit headCommit = env.getUnTillDbVCS().setFileContent(crb.getName(), Utils.VER_FILE_NAME,
				crb.getVersion().toNextPatch().toNextPatch().toString(), "version bumped");

		// set tag on unTillDb. Version file should not be changed
		action = execAndGetActionTag(compUnTillDb, null);
		assertActionDoesTag(action, compUnTillDb);

		assertEquals(headCommit, env.getUnTillDbVCS().getHeadCommit(crb.getName()));
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


