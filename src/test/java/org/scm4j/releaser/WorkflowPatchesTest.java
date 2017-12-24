package org.scm4j.releaser;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranchCurrent;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.branch.ReleaseBranchPatch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.exceptions.ENoReleaseBranchForPatch;
import org.scm4j.releaser.exceptions.ENoReleases;
import org.scm4j.releaser.exceptions.EReleaseMDepsNotLocked;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.WalkDirection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class WorkflowPatchesTest extends WorkflowTestBase {

	@Test
	public void testPatches() throws Exception {
		forkAndBuild(compUnTill);

		// add feature to existing unTillDb release
		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(compUnTillDb, repoFactory);
		env.generateFeatureCommit(env.getUnTillDbVCS(), crb.getName(), "patch feature added");

		// build unTillDb patch
		Component compUnTillDbPatch = new Component(UNTILLDB + ":" + env.getUnTillDbVer().toRelease(), repoFactory);
		IAction action = execAndGetActionBuild(compUnTillDbPatch);
		assertActionDoesBuild(action, compUnTillDb);

		ReleaseBranchPatch rb = ReleaseBranchFactory.getReleaseBranchPatch(compUnTillDbPatch, repoFactory);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch().toNextPatch(),
				rb.getVersion());
		ExtendedStatusBuilder builder = new ExtendedStatusBuilder(repoFactory);
		assertEquals(BuildStatus.DONE, builder.getAndCacheMinorStatus(compUnTillDbPatch).getStatus());

		// Existing unTill and UBL release branches should actualize its mdeps
		action = execAndGetActionBuild(compUnTill.clone(env.getUnTillVer().toRelease()));
		assertActionDoesBuild(action, compUBL, BuildStatus.ACTUALIZE_PATCHES);
		assertActionDoesBuild(action, compUnTill, BuildStatus.BUILD_MDEPS);
		assertActionDoesNothing(action, compUnTillDb);

		// check unTill uses new untillDb and UBL versions in existing unTill release branch.
		rb = ReleaseBranchFactory.getReleaseBranchPatch(compUnTill.clone(env.getUnTillVer().toRelease()), repoFactory);
		
		List<Component> mdeps = rb.getMDeps();
		for (Component mdep : mdeps) {
			if (mdep.getName().equals(UBL)) {
				assertEquals(env.getUblVer().toReleaseZeroPatch().toNextPatch(), mdep.getVersion());
			} else if (mdep.getName().equals(UNTILLDB)) {
				assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), mdep.getVersion());
			} else {
				fail();
			}
		}
	}

	@Test
	public void testBuildPatchOnExistingRelease() throws Exception {
		// 2.59
		forkAndBuild(compUnTillDb);

		// 2.60
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature added");
		forkAndBuild(compUnTillDb, 2);

		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(compUnTillDb, repoFactory);
		assertEquals(env.getUnTillDbVer().toNextMinor().toRelease(), crb.getVersion());

		// add feature for 2.59.1
		Component compToPatch = new Component(UNTILLDB + ":2.59.1", repoFactory);
		ReleaseBranchPatch rb = ReleaseBranchFactory.getReleaseBranchPatch(compToPatch, repoFactory);
		env.generateFeatureCommit(env.getUnTillDbVCS(), rb.getName(), "2.59.1 feature merged");

		// build new unTillDb patch 2.59.1
		IAction action = execAndGetActionBuild(compToPatch);
		assertActionDoesBuild(action, compUnTillDb);
		rb = ReleaseBranchFactory.getReleaseBranchPatch(compToPatch, repoFactory);
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toPreviousMinor().toNextPatch().toRelease(), rb.getVersion());
	}
	
	@Test
	public void testExceptionOnPatchOnUnexistingBranch() {
		// try do build a patch for unreleased version
		Component compWithUnexistingVersion = new Component(UNTILLDB + ":2.70.0", repoFactory);
		try {
			execAndGetActionBuild(compWithUnexistingVersion);
			fail();
		} catch (ENoReleaseBranchForPatch e) {
		}
	}

	@Test
	public void testExceptionOnPatchOnUnreleasedComponent() {
		fork(compUnTillDb);

		// try to build a patch on existing branch with no releases
		Component compUnTillDbVersioned = compUnTillDb.clone(env.getUnTillDbVer().toReleaseZeroPatch());
		try {
			execAndGetActionBuild(compUnTillDbVersioned);
			fail();
		} catch(ENoReleases e) {
		}
	}

	@Test
	public void testExceptionMDepsNotLockedOnPatch() {
		forkAndBuild(compUnTillDb);

		// simulate non-locked mdep
		Component nonLockedMDep = new Component(UNTILL, repoFactory);
		MDepsFile mdf = new MDepsFile(UNTILL);
		ReleaseBranchCurrent rb = ReleaseBranchFactory.getCRB(compUnTillDb, repoFactory);
		env.getUnTillDbVCS().setFileContent(rb.getName(), Utils.MDEPS_FILE_NAME,
				mdf.toFileContent(), "mdeps file added");

		// try to build patch
		try {
			execAndGetActionBuild(compUnTillDb.clone(rb.getVersion()));
			fail();
		} catch (EReleaseMDepsNotLocked e) {
			assertThat(e.getNonLockedMDeps(), Matchers.<Collection<Component>>allOf(
					Matchers.hasSize(1),
					Matchers.contains(nonLockedMDep)));
		}
	}

	@Test
	public void testPatchDONEIfLastCommitTagged() throws Exception {
		// fork unTillDb
		forkAndBuild(compUnTillDb);

		// add an igonored feature and tag it
		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(compUnTillDb, repoFactory);
		Component compUnTillDbVersioned = compUnTillDb.clone(crb.getVersion());
		env.generateFeatureCommit(env.getUnTillDbVCS(), crb.getName(), LogTag.SCM_IGNORE + " feature megred");
		env.getUnTillDbVCS().createTag(crb.getName(), "tag", "tag", null);

		ExtendedStatusBuilder statusBuilder = new ExtendedStatusBuilder(repoFactory);
		ExtendedStatus status = statusBuilder.getAndCachePatchStatus(compUnTillDbVersioned, new CachedStatuses());
		assertEquals(BuildStatus.DONE, status.getStatus());
	}

	@Test
	public void testPatchDONEIfAllReleaseBranchCommitsIgnored() throws Exception {
		forkAndBuild(compUnTillDb);

		// simulate no commits left in release branch, i.e. all igonred and no tags.
		// loop in noValueableCommitsAfterLastTag should be interrupted
		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(compUnTillDb, repoFactory);
		Component mockedCompVersioned = spy(compUnTillDb.clone(crb.getVersion()));
		IVCS mockedVCS = spy(env.getUnTillDbVCS());
		doReturn(mockedVCS).when(mockedCompVersioned).getVCS();
		doReturn(new ArrayList<VCSCommit>()).when(mockedVCS)
				.getCommitsRange(anyString(), (String) isNull(), any(WalkDirection.class), anyInt());

		ExtendedStatusBuilder statusBuilder = new ExtendedStatusBuilder(repoFactory);
		ExtendedStatus status = statusBuilder.getAndCachePatchStatus(mockedCompVersioned, new CachedStatuses());
		assertEquals(BuildStatus.DONE, status.getStatus());
	}
}
