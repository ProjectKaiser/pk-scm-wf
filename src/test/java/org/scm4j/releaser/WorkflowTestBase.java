package org.scm4j.releaser;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.Option;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.scmactions.SCMActionBuild;
import org.scm4j.releaser.scmactions.SCMActionFork;
import org.scm4j.releaser.scmactions.SCMActionTagRelease;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

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
		TestBuilder.setBuilders(new HashMap<String, TestBuilder>());
		new DelayedTagsFile().delete();
		FileUtils.deleteDirectory(ReleaseBranch.RELEASES_DIR);
	}

	@After
	public void tearDown() throws Exception {
		if (env != null) {
			env.close();
		}
		TestBuilder.setBuilders(null);
		Options.setOptions(new ArrayList<Option>());
		FileUtils.deleteDirectory(ReleaseBranch.RELEASES_DIR);
	}
	
	public void checkUnTillDbBuilt(int times) {
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		assertNotNull(TestBuilder.getBuilders());
		assertNotNull(TestBuilder.getBuilders().get(UNTILLDB));
		
		assertTrue(rbUnTillDb.getBuildDir().exists());

		// check versions
		Version verRelease = rbUnTillDb.getVersion();
		Version expectedReleaseVer = env.getUnTillDbVer().toReleaseZeroPatch().toPreviousMinor().toNextPatch();
		for (int i = 0; i < times; i++) {
			expectedReleaseVer = expectedReleaseVer.toNextMinor();
		}
		assertEquals(expectedReleaseVer, verRelease);

		// check tags
		List<VCSCommit> commits = env.getUnTillDbVCS().getCommitsRange(rbUnTillDb.getName(), null, WalkDirection.DESC, 2);
		List<VCSTag> tags = env.getUnTillDbVCS().getTagsOnRevision(commits.get(1).getRevision());
		assertTrue(tags.size() == 1);
		VCSTag tag = tags.get(0);
		assertEquals(verRelease.toPreviousPatch().toString(), tag.getTagName());
	}
	
	public void checkUnTillDbBuilt() {
		checkUnTillDbBuilt(1);
	}

	public void checkUBLBuilt() {
		checkUnTillDbBuilt();
		ReleaseBranch rbUBL = new ReleaseBranch(compUBL);
		assertNotNull(TestBuilder.getBuilders());
		assertNotNull(TestBuilder.getBuilders().get(UBL));

		assertTrue(rbUBL.getBuildDir().exists());
		
		// check UBL versions
		assertEquals(env.getUblVer().toNextMinor(), dbUBL.getVersion());
		assertEquals(env.getUblVer().toReleaseZeroPatch(), rbUBL.getVersion().toReleaseZeroPatch());

		// check UBL mDeps
		List<Component> ublReleaseMDeps = rbUBL.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch(), ublReleaseMDeps.get(0).getVersion().toReleaseZeroPatch());

		// check tags
		List<VCSTag> tags = env.getUblVCS().getTags();
		assertTrue(tags.size() == 1);
		VCSTag tag = tags.get(0);
		assertEquals(dbUBL.getVersion().toPreviousMinor().toReleaseZeroPatch().toString(), tag.getTagName());
		List<VCSCommit> commits = env.getUblVCS().getCommitsRange(rbUBL.getName(), null, WalkDirection.DESC, 2);
		assertEquals(commits.get(1), tag.getRelatedCommit());
	}


	public void checkUBLForked() {
		ReleaseBranch rbUBL = new ReleaseBranch(compUBL);
		// check branches
		assertTrue(env.getUblVCS().getBranches(compUBL.getVcsRepository().getReleaseBranchPrefix()).contains(rbUBL.getName()));

		// check versions
		Version verTrunk = dbUBL.getVersion();
		Version verRelease = rbUBL.getVersion();
		assertEquals(env.getUblVer().toNextMinor(),verTrunk);
		assertEquals(env.getUblVer().toReleaseZeroPatch(), verRelease);

		// check mDeps
		List<Component> ublReleaseMDeps = rbUBL.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		// do not consider patch because unTillDb could be build already befor UBL fork so target patch is unknown
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toReleaseZeroPatch(), ublReleaseMDeps.get(0).getVersion().toReleaseZeroPatch()); 
	}
	
	public void checkUnTillDbForked(int times) {
		ReleaseBranch newUnTillDbrb = new ReleaseBranch(compUnTillDb);

		// check branches
		assertTrue(env.getUnTillDbVCS().getBranches(compUnTillDb.getVcsRepository().getReleaseBranchPrefix()).contains(newUnTillDbrb.getName()));

		// check versions.
		Version verTrunk = dbUnTillDb.getVersion();
		Version verRelease = newUnTillDbrb.getVersion();
		Version expectedTrunkVer = env.getUnTillDbVer();
		Version expectedReleaseVer = env.getUnTillDbVer().toReleaseZeroPatch().toPreviousMinor();
		for (int i = 0; i < times; i++) {
			expectedTrunkVer = expectedTrunkVer.toNextMinor();
			expectedReleaseVer = expectedReleaseVer.toNextMinor();
		}
		assertEquals(expectedTrunkVer, verTrunk);
		assertEquals(expectedReleaseVer, verRelease);
	}

	public void checkUnTillDbForked() {
		checkUnTillDbForked(1);
	}

	public void checkUnTillForked() {
		checkUnTillDbForked();
		checkUBLForked();

		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill);

		// check branches
		assertTrue(env.getUnTillVCS().getBranches(compUnTill.getVcsRepository().getReleaseBranchPrefix()).contains(rbUnTill.getName()));

		// check versions
		Version verTrunk = dbUnTill.getVersion();
		Version verRelease = rbUnTill.getVersion();
		assertEquals(env.getUnTillVer().toNextMinor(), verTrunk);
		assertEquals(env.getUnTillVer().toReleaseZeroPatch(), verRelease);

		// check mDeps
		List<Component> unTillReleaseMDeps = rbUnTill.getMDeps();
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

	public void checkUnTillBuilt() {
		checkUBLBuilt();
		ReleaseBranch rbUnTill= new ReleaseBranch(compUnTill);
		
		assertTrue(rbUnTill.getBuildDir().exists());
		
		// check versions
		assertEquals(env.getUnTillVer().toNextMinor(), dbUnTill.getVersion());
		assertEquals(env.getUnTillVer().toReleaseZeroPatch(), rbUnTill.getVersion().toReleaseZeroPatch());

		// check mDeps
		List<Component> untillReleaseMDeps = rbUnTill.getMDeps();
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
		List<VCSCommit> commits = env.getUnTillVCS().getCommitsRange(rbUnTill.getName(), null, WalkDirection.DESC, 2);
		assertEquals(commits.get(1), tag.getRelatedCommit());
	}
	
	protected IProgress getProgress(IAction action) {
		return new ProgressConsole(action.toString(), ">>> ", "<<< ");
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

	protected void assertIsGoingToFork(IAction action, Component... comps) {
		assertThat(action, allOf(
				instanceOf(SCMActionFork.class),
				hasProperty("mbs", equalTo(BuildStatus.FORK))), comps);
	}

	protected void assertIsGoingToBuild(IAction action, Component... comps) {
		assertThat(action, allOf(
				instanceOf(SCMActionBuild.class),
				hasProperty("mbs", equalTo(BuildStatus.BUILD))), comps);
	}

	protected void assertIsGoingToBuild(IAction action, Component comp, BuildStatus mbs) {
		assertThat(action, allOf(
				instanceOf(SCMActionBuild.class),
				hasProperty("mbs", equalTo(mbs))), comp);
	}

	protected void assertIsGoingToTag(IAction action, Component comp) {
		assertThat(action, instanceOf(SCMActionTagRelease.class), comp);
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
