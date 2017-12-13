package org.scm4j.releaser;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.MDepsSource;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.scmactions.SCMActionRelease;
import org.scm4j.releaser.scmactions.SCMActionTag;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;
import org.scm4j.vcs.api.exceptions.EVCSBranchNotFound;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

public class WorkflowTestBase {
	protected TestEnvironment env;
	protected static final String UNTILL = TestEnvironment.PRODUCT_UNTILL;
	protected static final String UNTILLDB = TestEnvironment.PRODUCT_UNTILLDB;
	protected static final String UBL = TestEnvironment.PRODUCT_UBL;
	protected Component compUnTill;
	protected Component compUnTillDb;
	protected Component compUBL;
	protected DevelopBranch dbUnTill;
	protected DevelopBranch dbUnTillDb;
	protected DevelopBranch dbUBL;

	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		compUnTill = new Component(UNTILL);
		compUnTillDb = new Component(UNTILLDB);
		compUBL = new Component(UBL);
		dbUnTill = new DevelopBranch(compUnTill);
		dbUnTillDb = new DevelopBranch(compUnTillDb);
		dbUBL = new DevelopBranch(compUBL);
		TestBuilder.setBuilders(new HashMap<>());
		new DelayedTagsFile().delete();
		Utils.waitForDeleteDir(MDepsSource.RELEASES_DIR);
	}

	@After
	public void tearDown() throws Exception {
		if (env != null) {
			env.close();
		}
		TestBuilder.setBuilders(null);
		Utils.waitForDeleteDir(MDepsSource.RELEASES_DIR);
	}

	protected Version getCrbNextVersion(Component comp) {
		IVCS vcs = comp.getVCS();
		Version crbFirstVersion = Utils.getDevVersion(comp).toPreviousMinor().toReleaseZeroPatch();
		Version latestVersion;
		try {
			latestVersion = new Version(vcs.getFileContent(Utils.getReleaseBranchName(comp, crbFirstVersion), SCMReleaser.VER_FILE_NAME, null));
		} catch (EVCSBranchNotFound | EVCSFileNotFound e) {
			latestVersion = crbFirstVersion;
		}
		return latestVersion;
	}
	
	public void checkUnTillDbBuilt(int times) {
		Version latestVersion = getCrbNextVersion(compUnTillDb);
		
		assertNotNull(TestBuilder.getBuilders());
		assertNotNull(TestBuilder.getBuilders().get(UNTILLDB));
		
		assertTrue(Utils.getBuildDir(compUnTillDb, latestVersion).exists());

		// check versions
		Version expectedReleaseVer = env.getUnTillDbVer().toReleaseZeroPatch().toPreviousMinor().toNextPatch();
		for (int i = 0; i < times; i++) {
			expectedReleaseVer = expectedReleaseVer.toNextMinor();
		}
		assertEquals(expectedReleaseVer, latestVersion);

		// check tags
		List<VCSCommit> commits = env.getUnTillDbVCS().getCommitsRange(Utils.getReleaseBranchName(compUnTillDb, latestVersion), null, WalkDirection.DESC, 2);
		List<VCSTag> tags = env.getUnTillDbVCS().getTagsOnRevision(commits.get(1).getRevision());
		assertTrue(tags.size() == 1);
		VCSTag tag = tags.get(0);
		assertEquals(latestVersion.toPreviousPatch().toString(), tag.getTagName());
	}

	public void checkUnTillDbBuilt() {
		checkUnTillDbBuilt(1);
	}
	
	protected void checkUBLBuilt(int times) {
		checkUnTillDbBuilt(times);
		Version latestVersion = getCrbNextVersion(compUBL);
		
		assertNotNull(TestBuilder.getBuilders());
		assertNotNull(TestBuilder.getBuilders().get(UBL));

		assertTrue(Utils.getBuildDir(compUBL, latestVersion).exists());
		
		// check UBL versions
		Version expectedReleaseVer = env.getUblVer().toReleaseZeroPatch().toPreviousMinor().toNextPatch();
		for (int i = 0; i < times; i++) {
			expectedReleaseVer = expectedReleaseVer.toNextMinor();
		}
		assertEquals(expectedReleaseVer, latestVersion);
		
		// check UBL mDeps
		List<Component> ublReleaseMDeps = getReleaseBranchMDeps(compUBL, latestVersion);
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		Version expectedUnTillDbReleaseVer = env.getUnTillDbVer().toReleaseZeroPatch().toPreviousMinor();
		for (int i = 0; i < times; i++) {
			expectedUnTillDbReleaseVer = expectedUnTillDbReleaseVer.toNextMinor();
		}
		assertEquals(expectedUnTillDbReleaseVer, ublReleaseMDeps.get(0).getVersion());

		// check tags
		List<VCSTag> tags = env.getUblVCS().getTags();
		assertTrue(tags.size() == times);
		
		// check has tags for each built version
		Version expectedUBLReleaseVer = env.getUblVer().toReleaseZeroPatch().toPreviousMinor();
		for (int i = 0; i < times; i++) {
			expectedUBLReleaseVer = expectedUBLReleaseVer.toNextMinor();
			assertTrue(hasTagForVersion(tags, expectedUBLReleaseVer));
		}
		
		// check if the pre-last commit of each release branch is tagged 
		for (VCSTag tag : tags) {
			List<VCSCommit> commits = env.getUblVCS().getCommitsRange(Utils.getReleaseBranchName(compUBL, new Version(tag.getTagName())), null, WalkDirection.DESC, 2);
			assertEquals(commits.get(1), tag.getRelatedCommit());	
		}
		
	}

	private boolean hasTagForVersion(List<VCSTag> tags, Version expectedUBLReleaseVer) {
		for (VCSTag tag : tags) {
			if (tag.getTagName().equals(expectedUBLReleaseVer.toString())) {
				return true;
			}
		}
		return false;
	}

	protected void checkUBLBuilt() {
		checkUBLBuilt(1);
	}

	protected List<Component> getReleaseBranchMDeps(Component comp, Version forVersion) {
		try {
			return new MDepsFile(comp.getVCS().getFileContent(Utils.getReleaseBranchName(comp, forVersion), SCMReleaser.MDEPS_FILE_NAME, null)).getMDeps();
		} catch(EVCSFileNotFound | EVCSBranchNotFound e) {
			throw new RuntimeException(Utils.getReleaseBranchName(comp, forVersion) + " branch does not exist");
		}
	}

	protected void checkUBLForked() {
		checkUBLForked(1);
	}

	public void checkUBLForked(int times) {
		Version latestVersion = getCrbNextVersion(compUBL);
		// check branches
		assertTrue(env.getUblVCS().getBranches(compUBL.getVcsRepository().getReleaseBranchPrefix()).contains(Utils.getReleaseBranchName(compUBL, latestVersion)));

		// check versions
		Version trunkVersion = dbUBL.getVersion();
		Version expectedTrunkVer = env.getUblVer();
		Version expectedReleaseVer = env.getUblVer().toReleaseZeroPatch().toPreviousMinor();
		for (int i = 0; i < times; i++) {
			expectedTrunkVer = expectedTrunkVer.toNextMinor();
			expectedReleaseVer = expectedReleaseVer.toNextMinor();
		}
		assertEquals(expectedTrunkVer, trunkVersion);
		assertEquals(expectedReleaseVer, latestVersion);

		// check mDeps
		List<Component> ublReleaseMDeps = getReleaseBranchMDeps(compUBL, latestVersion);
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		// do not consider patch because unTillDb could be build already befor UBL fork so target patch is unknown
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toReleaseZeroPatch(), ublReleaseMDeps.get(0).getVersion().toReleaseZeroPatch()); 
	}
	
	public void checkUnTillDbForked(int times) {
		Version latestVersion = getCrbNextVersion(compUnTillDb);
		// check branches
		assertTrue(env.getUnTillDbVCS().getBranches(compUnTillDb.getVcsRepository().getReleaseBranchPrefix()).contains(Utils.getReleaseBranchName(compUnTillDb, latestVersion)));

		// check versions.
		Version trunkVersion = dbUnTillDb.getVersion();
		Version expectedTrunkVer = env.getUnTillDbVer();
		Version expectedReleaseVer = env.getUnTillDbVer().toReleaseZeroPatch().toPreviousMinor();
		for (int i = 0; i < times; i++) {
			expectedTrunkVer = expectedTrunkVer.toNextMinor();
			expectedReleaseVer = expectedReleaseVer.toNextMinor();
		}
		assertEquals(expectedTrunkVer, trunkVersion);
		assertEquals(expectedReleaseVer, latestVersion);
	}

	public void checkUnTillDbForked() {
		checkUnTillDbForked(1);
	}

	public void checkUnTillOnlyForked(int times) {
		Version latestVersion = getCrbNextVersion(compUnTill);

		// check branches
		assertTrue(env.getUnTillVCS().getBranches(compUnTill.getVcsRepository().getReleaseBranchPrefix()).contains(Utils.getReleaseBranchName(compUnTill, latestVersion)));

		// check versions
		Version trunkVersion = dbUnTill.getVersion();
		Version expectedTrunkVer = env.getUnTillVer();
		Version expectedReleaseVer = env.getUnTillVer().toReleaseZeroPatch().toPreviousMinor();
		for (int i = 0; i < times; i++) {
			expectedTrunkVer = expectedTrunkVer.toNextMinor();
			expectedReleaseVer = expectedReleaseVer.toNextMinor();
		}
		assertEquals(expectedTrunkVer, trunkVersion);
		assertEquals(expectedReleaseVer, latestVersion);

		// check mDeps
		List<Component> unTillReleaseMDeps = getReleaseBranchMDeps(compUnTill, latestVersion);
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component unTillReleaseMDep : unTillReleaseMDeps) {
			if (unTillReleaseMDep.getName().equals(UBL)) {
				assertEquals(env.getUblVer().toReleaseZeroPatch(), unTillReleaseMDep.getVersion());
			} else if (unTillReleaseMDep.getName().equals(UNTILLDB)) {
				assertEquals(env.getUnTillDbVer().toReleaseZeroPatch(), unTillReleaseMDep.getVersion());
			} else {
				fail();
			}
		}
	}

	public void checkUnTillForked() {
		checkUnTillDbForked();
		checkUBLForked();
		checkUnTillOnlyForked(1);
	}

	public void checkUBLNotBuilt() {
		checkUnTillDbNotBuilt();
		Version latestVersion = getCrbNextVersion(compUnTillDb);
		assertEquals(Utils.ZERO_PATCH, latestVersion.getPatch());
		assertTrue(env.getUblVCS().getTags().isEmpty());
	}

	public void checkUnTillDbNotBuilt() {
		Version latestVersion = getCrbNextVersion(compUnTillDb);
		assertEquals(Utils.ZERO_PATCH, latestVersion.getPatch());
		assertTrue(env.getUnTillDbVCS().getTags().isEmpty());
	}

	public void checkUnTillNotBuilt() {
		checkUBLNotBuilt();
		Version latestVersion = getCrbNextVersion(compUnTill);
		assertEquals(Utils.ZERO_PATCH, latestVersion.getPatch());
		assertTrue(env.getUnTillVCS().getTags().isEmpty());
	}

	public void checkUnTillBuilt() {
		checkUBLBuilt();
		Version latestVersion = getCrbNextVersion(compUnTill);
		
		assertTrue(Utils.getBuildDir(compUnTill, latestVersion).exists());
		
		// check versions
		assertEquals(env.getUnTillVer().toNextMinor(), dbUnTill.getVersion());
		assertEquals(env.getUnTillVer().toReleaseZeroPatch(), latestVersion.toReleaseZeroPatch());

		// check mDeps
		List<Component> untillReleaseMDeps = getReleaseBranchMDeps(compUnTill, latestVersion);
		assertTrue(untillReleaseMDeps.size() == 2);
		assertEquals(compUnTillDb.getName(), untillReleaseMDeps.get(1).getName());
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch(), untillReleaseMDeps.get(1).getVersion().toReleaseZeroPatch());
		assertEquals(compUBL.getName(), untillReleaseMDeps.get(0).getName());
		assertEquals(env.getUblVer().toReleaseZeroPatch(), untillReleaseMDeps.get(0).getVersion().toReleaseZeroPatch());

		// check tags
		List<VCSTag> tags = env.getUnTillVCS().getTags();
		assertTrue(tags.size() == 1);
		VCSTag tag = tags.get(0);
		assertEquals(dbUnTill.getVersion().toPreviousMinor().toReleaseZeroPatch().toString(), tag.getTagName());
		List<VCSCommit> commits = env.getUnTillVCS().getCommitsRange(Utils.getReleaseBranchName(compUnTill, latestVersion), null, WalkDirection.DESC, 2);
		assertEquals(commits.get(1), tag.getRelatedCommit());
	}
	
	protected IProgress getProgress(IAction action) {
		return new ProgressConsole(action.toStringAction(), ">>> ", "<<< ");
	}

	private IAction getActionByComp(IAction action, Component comp, int level) {
		for (IAction nestedAction : action.getChildActions()) {
			IAction res = getActionByComp(nestedAction, comp, level + 1);
			if (res != null) {
				return res;
			}
		}
		if (action.getComp().getName().equals(comp.getName())) {
			return action;
		}
		if (level == 0) {
			throw new AssertionError("No action for " + comp);
		}
		return null;
	}

	private IAction getActionByComp(IAction action, Component comp) {
		return getActionByComp(action, comp, 0);
	}

	protected void assertThat(IAction action, Matcher<? super IAction> matcher, Component... comps) {
		for (Component comp : comps) {
			IAction actionForComp = getActionByComp(action, comp);
			Assert.assertThat("action for " + comp, actionForComp, matcher);
		}
	}

	protected void assertIsGoingToForkAll(IAction action) {
		assertIsGoingToFork(action, compUBL, compUnTillDb, compUnTill);
	}

	protected void assertIsGoingToForkAndBuildAll(IAction action) {
		assertIsGoingToForkAndBuild(action, compUBL, compUnTillDb, compUnTill);
	}

	protected void assertIsGoingToFork(IAction action, Component... comps) {
		assertThat(action, allOf(
				instanceOf(SCMActionRelease.class),
				hasProperty("bsFrom", equalTo(BuildStatus.FORK)), 
				hasProperty("bsTo", equalTo(BuildStatus.LOCK))), comps);
	}

	protected void assertIsGoingToForkAndBuild(IAction action, Component... comps) {
		assertTrue(comps.length > 0);
		assertThat(action, allOf(
				instanceOf(SCMActionRelease.class),
				hasProperty("bsFrom", equalTo(BuildStatus.FORK)),
				hasProperty("bsTo", equalTo(BuildStatus.BUILD))), comps);
	}

	protected void assertIsGoingToBuild(IAction action, Component comp, BuildStatus mbs) {
		assertThat(action, allOf(
				instanceOf(SCMActionRelease.class),
				hasProperty("bsFrom", equalTo(mbs)),
				hasProperty("bsTo", equalTo(BuildStatus.BUILD))), comp);
	}
	
	protected void assertIsGoingToBuild(IAction action, Component... comps) {
		assertThat(action, allOf(
				instanceOf(SCMActionRelease.class),
				hasProperty("bsFrom", equalTo(BuildStatus.BUILD)), 
				hasProperty("bsTo", equalTo(BuildStatus.BUILD))), comps);
	}

	protected void assertIsGoingToDoNothing(IAction action, BuildStatus bsFrom, BuildStatus bsTo, Component... comps) {
		assertThat(action, allOf(
				instanceOf(SCMActionRelease.class),
				hasProperty("bsFrom", equalTo(bsFrom)),
				hasProperty("bsTo", equalTo(bsTo)),
				hasProperty("procs", empty())), comps);
	}

	protected void assertIsGoingToDoNothing(IAction action, Component... comps) {
		assertIsGoingToDoNothing(action, BuildStatus.DONE, null, comps);
	}

	protected void assertIsGoingToTag(IAction action, Component comp) {
		assertThat(action, instanceOf(SCMActionTag.class), comp);
	}

	protected void assertIsGoingToBuildAll(IAction action) {
		assertIsGoingToBuild(action, compUnTillDb, BuildStatus.BUILD);
		assertIsGoingToBuild(action, compUnTill, BuildStatus.BUILD_MDEPS);
		assertIsGoingToBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
	}

	protected void assertIsGoingToTagAll(IAction action) {
		assertIsGoingToTag(action, compUnTillDb);
		assertIsGoingToTag(action, compUnTill);
		assertIsGoingToTag(action, compUBL);
	}
}
