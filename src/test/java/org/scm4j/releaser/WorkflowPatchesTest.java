package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranchCurrent;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.branch.ReleaseBranchPatch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.conf.VCSRepositoryFactory;
import org.scm4j.releaser.exceptions.EMinorUpgradeDowngrade;
import org.scm4j.releaser.exceptions.ENoReleaseBranchForPatch;
import org.scm4j.releaser.exceptions.ENoReleases;
import org.scm4j.releaser.exceptions.EReleaseMDepsNotLocked;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.WalkDirection;

public class WorkflowPatchesTest extends WorkflowTestBase {

	@Test
	public void testPatches() throws Exception {
		forkAndBuild(compUnTill);

		// add feature to existing unTillDb release
		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), crb.getName(), "patch feature added");

		// build unTillDb patch
		Component compUnTillDbPatch = new Component(UNTILLDB + ":" + env.getUnTillDbVer().toRelease());
		IAction action = execAndGetActionBuild(compUnTillDbPatch);
		assertActionDoesBuild(action, compUnTillDbPatch);

		// check patch version
		ReleaseBranchPatch rb = ReleaseBranchFactory.getReleaseBranchPatch(compUnTillDbPatch.getVersion(),
				repoUnTillDb);
		Assert.assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch().toNextPatch(), rb.getVersion());

		// check nothing happens on next build
		action = execAndGetActionBuild(compUnTillDbPatch);
		assertActionDoesNothing(action, compUnTillDbPatch);

		// Existing unTill and UBL release branches should actualize its mdeps
		action = execAndGetActionBuild(compUnTill.clone(env.getUnTillVer().toRelease()));
		assertActionDoesBuild(action, compUBL, BuildStatus.ACTUALIZE_PATCHES);
		assertActionDoesBuild(action, compUnTill, BuildStatus.BUILD_MDEPS);
		assertActionDoesNothing(action, compUnTillDb);

		// check unTill uses new untillDb and UBL versions in existing unTill release
		// branch.
		rb = ReleaseBranchFactory.getReleaseBranchPatch(compUnTill.clone(env.getUnTillVer().toRelease()).getVersion(),
				repoUnTill);

		List<Component> mdeps = rb.getMDeps();
		for (Component mdep : mdeps) {
			if (mdep.getName().equals(UBL)) {
				Assert.assertEquals(env.getUblVer().toReleaseZeroPatch().toNextPatch(), mdep.getVersion());
			} else if (mdep.getName().equals(UNTILLDB)) {
				Assert.assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), mdep.getVersion());
			} else {
				fail();
			}
		}
	}

	@Test
	public void testBuildPatchOnPreviousRelease() throws Exception {
		// 2.59
		forkAndBuild(compUnTillDb);

		// 2.60
		env.generateFeatureCommit(env.getUnTillDbVCS(), repoUnTillDb.getDevelopBranch(), "feature added");
		forkAndBuild(compUnTillDb, 2);

		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		Assert.assertEquals(env.getUnTillDbVer().toNextMinor().toRelease(), crb.getVersion());

		// add feature for 2.59.1
		Component compToPatch = new Component(UNTILLDB + ":2.59.1");
		ReleaseBranchPatch rb = ReleaseBranchFactory.getReleaseBranchPatch(compToPatch.getVersion(), repoUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), rb.getName(), "2.59.1 feature merged");

		// build new unTillDb patch 2.59.2
		IAction action = execAndGetActionBuild(compToPatch);
		assertActionDoesBuild(action, compUnTillDb);
		rb = ReleaseBranchFactory.getReleaseBranchPatch(compToPatch.getVersion(), repoUnTillDb);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch().toNextPatch(), rb.getVersion());
	}

	@Test
	public void testExceptionOnPatchOnUnexistingBranch() {
		// try do build a patch for unreleased version
		Component compWithUnexistingVersion = new Component(UNTILLDB + ":2.70.0");
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
		} catch (ENoReleases e) {
		}
	}

	@Test
	public void testExceptionMDepsNotLockedOnPatch() {
		forkAndBuild(compUnTillDb);

		// simulate non-locked mdep
		Component nonLockedMDep = new Component(UNTILL);
		MDepsFile mdf = new MDepsFile(UNTILL);
		ReleaseBranchCurrent rb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		env.getUnTillDbVCS().setFileContent(rb.getName(), Constants.MDEPS_FILE_NAME, mdf.toFileContent(),
				"mdeps file added");

		// try to build patch
		try {
			execAndGetActionBuild(compUnTillDb.clone(rb.getVersion()));
			fail();
		} catch (EReleaseMDepsNotLocked e) {
			assertThat(e.getNonLockedMDeps(),
					Matchers.<Collection<Component>>allOf(Matchers.hasSize(1), Matchers.contains(nonLockedMDep)));
		}
	}

	@Test
	public void testPatchDONEIfAllReleaseBranchCommitsIgnored() {
		forkAndBuild(compUnTillDb);

		// simulate no commits left in release branch, i.e. all ignored and no tags.
		// do-while loop in noValueableCommitsAfterLastTag should be interrupted
		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		Component compVersioned = compUnTillDb.clone(crb.getVersion());
		IVCS mockedVCS = Mockito.spy(env.getUnTillDbVCS());
		VCSRepository mockedRepo = spy(repoFactory.getVCSRepository(compVersioned));
		doReturn(mockedVCS).when(mockedRepo).getVCS();
		doReturn(new ArrayList<VCSCommit>()).when(mockedVCS).getCommitsRange(anyString(), (String) isNull(),
				any(WalkDirection.class), anyInt());
		VCSRepositoryFactory mockedRepoFactory = spy(repoFactory);
		doReturn(mockedRepo).when(mockedRepoFactory).getVCSRepository(compVersioned);

		ExtendedStatusBuilder statusBuilder = new ExtendedStatusBuilder(mockedRepoFactory);
		ExtendedStatus status = statusBuilder.getAndCachePatchStatus(compVersioned, new CachedStatuses());
		assertEquals(BuildStatus.DONE, status.getStatus());
	}
	
	@Test
	public void testMinorUpgradeDowngradeException() {
		forkAndBuild(compUnTill);

		// release next 2.60 unTillDb minor
		env.generateFeatureCommit(env.getUnTillDbVCS(), repoUnTillDb.getDevelopBranch(), "feature added");
		forkAndBuild(compUnTillDb, 2);
		
		// make unTill use new 2.60.0 version of unTillDb
		ReleaseBranchCurrent crbUnTill = ReleaseBranchFactory.getCRB(repoUnTill);
		ReleaseBranchCurrent crbUBL = ReleaseBranchFactory.getCRB(repoUBL);
		ReleaseBranchCurrent crbUnTillDb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		MDepsFile mdf = new MDepsFile(env.getUnTillVCS().getFileContent(crbUnTill.getName(), Constants.MDEPS_FILE_NAME, null));
		for (Component mDep : mdf.getMDeps()) {
			if (mDep.clone("").equals(compUnTillDb)) {
				mdf.replaceMDep(mDep.clone(crbUnTillDb.getVersion().toPreviousPatch()));
			}
		}
		env.getUnTillVCS().setFileContent(crbUnTill.getName(), Constants.MDEPS_FILE_NAME, mdf.toFileContent(), "unTillDb version is changed manually");
		
		// unTill still have old untillDb version. untillDb for unTill is processed first and cached 2.60.0.
		// The UBL have unTillDb 2.59.0 but 2.60.0 is cached -> UBL status would be ACTUALIZE_PATCHES but EMinorUpgradeDowngrade should be thrown
		try {
			execAndGetActionBuild(compUnTill.clone(env.getUnTillVer().toRelease()));
			fail();
		} catch (EMinorUpgradeDowngrade e) {
			if (e.getRootComp().equals(compUBL.clone(crbUBL.getVersion().toPreviousPatch()))) {
				// on >1-core systems
				assertEquals(compUnTillDb.clone("2.59.0"), e.getProblematicMDep());
				assertEquals(new Version("2.60.0"), e.getChangeToVersion());
			} else if (e.getRootComp().equals(compUnTill.clone(env.getUnTillVer().toRelease()))) {
				// on 1-core systems
				assertEquals(compUnTillDb.clone("2.60.0"), e.getProblematicMDep());
				assertEquals(new Version("2.59.0"), e.getChangeToVersion());
			} else {
				fail();
			}
		}
	}

	@Test
	public void testPatchDowngradeException() {
		forkAndBuild(compUnTill);

		// unTill uses 2.59.0 version of UnTillDb
		// make UBL use 2.59.1 version of unTillDb
		ReleaseBranchCurrent crbUBL = ReleaseBranchFactory.getCRB(repoUBL);
		ReleaseBranchCurrent crbUnTillDb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		MDepsFile mdf = new MDepsFile(env.getUblVCS().getFileContent(crbUBL.getName(), Constants.MDEPS_FILE_NAME, null));
		assertEquals(1, mdf.getMDeps().size());
		mdf.replaceMDep(mdf.getMDeps().get(0).clone(crbUnTillDb.getVersion()));
		env.getUblVCS().setFileContent(crbUBL.getName(), Constants.MDEPS_FILE_NAME, mdf.toFileContent(), "unTillDb version is changed manually");

		// unTill still have 2.59.0 untillDb version. untillDb for unTill is processed first and cached 2.59.0.
		// The UBL have unTillDb 2.59.1 but 2.59.0 cached -> UBL status would be ACTUALIZE_PATCHES but EMinorUpgradeDowngrade should be thrown
		try {
			execAndGetActionBuild(compUnTill.clone(env.getUnTillVer().toRelease()));
			fail();
		} catch (EMinorUpgradeDowngrade e) {
			if (e.getRootComp().equals(compUBL.clone(crbUBL.getVersion().toPreviousPatch()))) {
				// on >1-core systems
				assertEquals(compUnTillDb.clone("2.59.1"), e.getProblematicMDep());
				assertEquals(new Version("2.59.0"), e.getChangeToVersion());
			} else if (e.getRootComp().equals(compUnTill.clone(env.getUnTillVer().toRelease()))) {
				// on 1-core systems
				assertEquals(compUnTillDb.clone("2.59.0"), e.getProblematicMDep());
				assertEquals(new Version("2.59.1"), e.getChangeToVersion());
			} else {
				fail();
			}
		}
	}
}
