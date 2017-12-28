package org.scm4j.releaser;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.builders.TestBuilder;
import org.scm4j.releaser.cli.CLI;
import org.scm4j.releaser.cli.CLICommand;
import org.scm4j.releaser.cli.Option;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.conf.VCSRepositoryFactory;
import org.scm4j.releaser.scmactions.SCMActionRelease;
import org.scm4j.releaser.scmactions.SCMActionTag;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

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
	protected VCSRepository repoUnTill;
	protected VCSRepository repoUnTillDb;
	protected VCSRepository repoUBL;
	protected VCSRepositoryFactory repoFactory;
	
	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		repoFactory = env.getRepoFactory();
		compUnTill = new Component(UNTILL);
		compUnTillDb = new Component(UNTILLDB);
		compUBL = new Component(UBL);
		repoUnTill = repoFactory.getVCSRepository(compUnTill);
		repoUnTillDb = repoFactory.getVCSRepository(compUnTillDb);
		repoUBL = repoFactory.getVCSRepository(compUBL);
		repoUnTill = repoFactory.getVCSRepository(compUnTill);
		dbUnTill = new DevelopBranch(compUnTill, repoUnTill);
		dbUnTillDb = new DevelopBranch(compUnTillDb, repoUnTillDb);
		dbUBL = new DevelopBranch(compUBL, repoUBL);
		TestBuilder.setBuilders(new HashMap<>());
		new DelayedTagsFile().delete();
		Utils.waitForDeleteDir(Utils.RELEASES_DIR);
	}

	@After
	public void tearDown() throws Exception {
		if (env != null) {
			env.close();
		}
		TestBuilder.setBuilders(null);
		Utils.waitForDeleteDir(Utils.RELEASES_DIR);
	}

	protected Version getCrbVersion(Component comp) {
		VCSRepository repo = repoFactory.getVCSRepository(comp);
		Version crbFirstVersion = Utils.getDevVersion(repo).toPreviousMinor().toReleaseZeroPatch();
		return new Version(repo.getVCS().getFileContent(Utils.getReleaseBranchName(repo, crbFirstVersion), Utils.VER_FILE_NAME, null));
	}

	private void checkCompBuilt(int times, Component comp, IVCS vcs, VCSRepository repo, Version ver) {
		Version latestVersion = getCrbVersion(comp);

		assertNotNull(TestBuilder.getBuilders().get(comp.getName()));

		assertTrue(Utils.getBuildDir(repo, latestVersion).exists());

		// check versions
		Version expectedReleaseVer = ver.toReleaseZeroPatch().toPreviousMinor().toNextPatch();
		for (int i = 0; i < times; i++) {
			expectedReleaseVer = expectedReleaseVer.toNextMinor();
		}
		assertEquals(expectedReleaseVer, latestVersion);
		Version expectedDevVer = expectedReleaseVer.toNextMinor().setPatch(ver.getPatch()).toSnapshot();
		assertEquals(expectedDevVer, Utils.getDevVersion(repo));

		// check tags
		List<VCSTag> tags = vcs.getTags();
		assertEquals(times, tags.size());

		// check has tags for each built version
		Version expectedCompReleaseVer = ver.toReleaseZeroPatch().toPreviousMinor();
		for (int i = 0; i < times; i++) {
			expectedCompReleaseVer = expectedCompReleaseVer.toNextMinor();
			assertTrue(hasTagForVersion(tags, expectedCompReleaseVer));
		}

		// check if the pre-last commit of each release branch is tagged
		for (VCSTag tag : tags) {
			List<VCSCommit> commits = vcs.getCommitsRange(Utils.getReleaseBranchName(
					repo, new Version(tag.getTagName())), null, WalkDirection.DESC, 2);
			assertEquals(commits.get(1), tag.getRelatedCommit());
		}
	}
	
	public void checkUnTillDbBuilt(int times) {
		checkCompBuilt(times, compUnTillDb, env.getUnTillDbVCS(), repoUnTillDb, env.getUnTillDbVer());
	}

	protected void checkUBLBuilt(int times) {
		checkUnTillDbBuilt(times);
		checkCompBuilt(times, compUBL, env.getUblVCS(), repoUBL, env.getUblVer());
		Version latestVersion = getCrbVersion(compUBL);

		// check UBL mDeps
		List<Component> ublReleaseMDeps = ReleaseBranchFactory.getMDepsRelease(
				Utils.getReleaseBranchName(repoUBL, latestVersion), repoUBL);
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		Version expectedUnTillDbReleaseVer = env.getUnTillDbVer().toReleaseZeroPatch().toPreviousMinor();
		for (int i = 0; i < times; i++) {
			expectedUnTillDbReleaseVer = expectedUnTillDbReleaseVer.toNextMinor();
		}
		assertEquals(expectedUnTillDbReleaseVer, ublReleaseMDeps.get(0).getVersion());
	}

	public void checkUnTillBuilt(int times) {
		checkUBLBuilt(times);
		checkCompBuilt(times, compUnTill, env.getUnTillVCS(), repoUnTill, env.getUnTillVer());
		Version latestVersion = getCrbVersion(compUnTill);

		// check mDeps
		List<Component> untillReleaseMDeps = ReleaseBranchFactory.getMDepsRelease(
				Utils.getReleaseBranchName(repoUnTill, latestVersion), repoUnTill);
		assertTrue(untillReleaseMDeps.size() == 2);
		assertEquals(compUnTillDb.getName(), untillReleaseMDeps.get(1).getName());
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch(), untillReleaseMDeps.get(1).getVersion().toReleaseZeroPatch());
		assertEquals(compUBL.getName(), untillReleaseMDeps.get(0).getName());
		assertEquals(env.getUblVer().toReleaseZeroPatch(), untillReleaseMDeps.get(0).getVersion().toReleaseZeroPatch());
	}

	private boolean hasTagForVersion(List<VCSTag> tags, Version expectedUBLReleaseVer) {
		for (VCSTag tag : tags) {
			if (tag.getTagName().equals(Utils.getTagDesc(expectedUBLReleaseVer.toString()).getName())) {
				return true;
			}
		}
		return false;
	}

	protected void checkUBLBuilt() {
		checkUBLBuilt(1);
	}

	protected void checkUBLForked() {
		checkUBLForked(1);
	}

	private void checkCompForked(int times, Component comp, Version ver, VCSRepository repo) {
		Version latestVersion = getCrbVersion(comp);
		// check branches
		assertTrue(env.getUnTillDbVCS().getBranches(repo.getReleaseBranchPrefix()).contains(
				Utils.getReleaseBranchName(repo, latestVersion)));

		// check versions.
		Version trunkVersion = Utils.getDevVersion(repo);
		Version expectedTrunkVer = ver;
		Version expectedReleaseVer = ver.toReleaseZeroPatch().toPreviousMinor();
		for (int i = 0; i < times; i++) {
			expectedTrunkVer = expectedTrunkVer.toNextMinor();
			expectedReleaseVer = expectedReleaseVer.toNextMinor();
		}
		assertEquals(expectedTrunkVer, trunkVersion);
		assertEquals(expectedReleaseVer, latestVersion);
	}

	public void checkUBLForked(int times) {
		Version latestVersion = getCrbVersion(compUBL);
		// check branches
		assertTrue(env.getUblVCS().getBranches(repoUBL.getReleaseBranchPrefix()).contains(
				Utils.getReleaseBranchName(repoUBL, latestVersion)));

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
		List<Component> ublReleaseMDeps = ReleaseBranchFactory.getMDepsRelease(
				Utils.getReleaseBranchName(repoUBL, latestVersion), repoUBL);
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		// do not consider patch because unTillDb could be build already before UBL fork so target patch is unknown
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toReleaseZeroPatch(), ublReleaseMDeps.get(0).getVersion().toReleaseZeroPatch()); 
	}
	
	public void checkUnTillDbForked(int times) {
		checkCompForked(times, compUnTillDb, env.getUnTillDbVer(), repoUnTillDb);
	}

	public void checkUnTillDbForked() {
		checkUnTillDbForked(1);
	}

	public void checkUnTillOnlyForked(int times) {
		Version latestVersion = getCrbVersion(compUnTill);

		// check branches
		assertTrue(env.getUnTillVCS().getBranches(repoUnTill.getReleaseBranchPrefix()).contains(
				Utils.getReleaseBranchName(repoUnTill, latestVersion)));

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
		List<Component> unTillReleaseMDeps = ReleaseBranchFactory.getMDepsRelease(
				Utils.getReleaseBranchName(repoUnTill, latestVersion), repoUnTill);
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

	protected void assertThatAction(IAction action, Matcher<? super IAction> matcher, Component... comps) {
		for (Component comp : comps) {
			IAction actionForComp = getActionByComp(action, comp);
			Assert.assertThat("action for " + comp, actionForComp, matcher);
		}
	}

	protected void assertActionDoesForkAll(IAction action) {
		assertActionDoesFork(action, getAllComps());
	}

	protected void assertActionDoesForkAndBuildAll(IAction action) {
		assertActionDoesForkAndBuild(action, getAllComps());
	}

	protected void assertActionDoesFork(IAction action, Component... comps) {
		assertThatAction(action, allOf(
				instanceOf(SCMActionRelease.class),
				hasProperty("bsFrom", equalTo(BuildStatus.FORK)), 
				hasProperty("bsTo", equalTo(BuildStatus.LOCK))), comps);
	}

	protected void assertActionDoesForkAndBuild(IAction action, Component... comps) {
		assertTrue(comps.length > 0);
		assertThatAction(action, allOf(
				instanceOf(SCMActionRelease.class),
				hasProperty("bsFrom", equalTo(BuildStatus.FORK)),
				hasProperty("bsTo", equalTo(BuildStatus.BUILD))), comps);
	}

	protected void assertActionDoesBuild(IAction action, Component comp, BuildStatus fromStatus) {
		assertThatAction(action, allOf(
				instanceOf(SCMActionRelease.class),
				hasProperty("bsFrom", equalTo(fromStatus)),
				hasProperty("bsTo", equalTo(BuildStatus.BUILD))), comp);
	}
	
	protected void assertActionDoesBuild(IAction action, Component... comps) {
		assertThatAction(action, allOf(
				instanceOf(SCMActionRelease.class),
				hasProperty("bsFrom", equalTo(BuildStatus.BUILD)), 
				hasProperty("bsTo", equalTo(BuildStatus.BUILD))), comps);
	}

	protected void assertActionDoesNothing(IAction action, BuildStatus bsFrom, BuildStatus bsTo, Component... comps) {
		assertThatAction(action, allOf(
				instanceOf(SCMActionRelease.class),
				hasProperty("bsFrom", equalTo(bsFrom)),
				hasProperty("bsTo", equalTo(bsTo)),
				hasProperty("procs", empty())), comps);
	}
	
	protected void assertActionDoesSkipAll(IAction action) {
		assertThatAction(action, allOf(
				instanceOf(SCMActionRelease.class),
				hasProperty("procs", empty())), getAllComps());
	}
	
	private Component[] getAllComps() {
		return new Component[] {compUBL, compUnTillDb, compUnTill};
	}

	protected void assertActionDoesNothing(IAction action, Component... comps) {
		assertActionDoesNothing(action, BuildStatus.DONE, null, comps);
	}

	protected void assertActionDoesTag(IAction action, Component comp) {
		assertThatAction(action, allOf(
				instanceOf(SCMActionTag.class), 
				hasProperty("childActions", empty())), comp);
	}

	protected void assertActionDoesBuildAll(IAction action) {
		assertActionDoesBuild(action, compUnTillDb, BuildStatus.BUILD);
		assertActionDoesBuild(action, compUnTill, BuildStatus.BUILD_MDEPS);
		assertActionDoesBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
	}

	protected IAction execAndGetActionFork(Component comp) {
		return execAndGetAction(CLICommand.FORK.getCmdLineStr(), comp.getCoords().toString());
	}
	
	protected IAction execAndGetActionBuild(Component comp) {
		return execAndGetAction(CLICommand.BUILD.getCmdLineStr(), comp.getCoords().toString());
	}
	
	private IAction getAndExecAction(Runnable preExec, String... args)  {
		CLI cli = new CLI();
		cli.setPreExec(preExec);
		if (cli.exec(args) != CLI.EXIT_CODE_OK) {
			throw cli.getLastException();
		};
		return cli.getAction();
	}

	private IAction execAndGetAction(String... args) {
		return getAndExecAction(null, args);
	}
	
	protected IAction execAndGetActionTag(Component comp, Runnable preExec) {
		return getAndExecAction(preExec, CLICommand.TAG.getCmdLineStr(), comp.getCoords().toString());
	}
	
	protected IAction execAndGetActionBuildDelayedTag(Component comp) {
		return execAndGetAction(CLICommand.BUILD.getCmdLineStr(), comp.getCoords().toString(), Option.DELAYED_TAG.getCmdLineStr());
	}
	
	protected void forkAndBuild(Component comp) {
		forkAndBuild(comp, 1);
	}
	
	protected void fork(Component comp) {
		fork(comp, 1);
	}
	
	protected void build(Component comp) {
		build(comp, 1);
	}
	
	protected void fork(Component comp, int times) {
		IAction action = execAndGetActionFork(comp);
		if (TestEnvironment.PRODUCT_UNTILL.contains(comp.getCoords().toString(""))) {
			assertActionDoesForkAll(action);
		} else if (TestEnvironment.PRODUCT_UBL.contains(comp.getCoords().toString(""))) {
			assertActionDoesFork(action, compUBL, compUnTillDb);
		} else if (TestEnvironment.PRODUCT_UNTILLDB.contains(comp.getCoords().toString(""))) {
			assertActionDoesFork(action, compUnTillDb);
		} else {
			fail("unexpected coords: " + comp.getCoords());
		}
		if (TestEnvironment.PRODUCT_UNTILL.contains(comp.getCoords().toString(""))) {
			if (times > 1) {
				fail("unsupported check unTill builds amount: " + times);
			}
			checkUnTillForked();
		} else if (TestEnvironment.PRODUCT_UBL.contains(comp.getCoords().toString(""))) {
			checkUBLForked(times);
		} else if (TestEnvironment.PRODUCT_UNTILLDB.contains(comp.getCoords().toString(""))) {
			checkUnTillDbForked(times);
		} else {
			fail("unexpected coords: " + comp.getCoords());
		}
	}
	
	protected void build(Component comp, int times) {
		IAction action = execAndGetActionBuild(comp);
		if (TestEnvironment.PRODUCT_UNTILL.contains(comp.getCoords().toString(""))) {
			assertActionDoesBuildAll(action);
		} else if (TestEnvironment.PRODUCT_UBL.contains(comp.getCoords().toString(""))) {
			assertActionDoesBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
			assertActionDoesBuild(action, compUnTillDb);
		} else if (TestEnvironment.PRODUCT_UNTILLDB.contains(comp.getCoords().toString(""))) {
			assertActionDoesBuild(action, compUnTillDb);
		} else {
			fail("unexpected coords: " + comp.getCoords());
		}
		if (TestEnvironment.PRODUCT_UNTILL.contains(comp.getCoords().toString(""))) {
			if (times > 1) {
				fail("unsupported check unTill builds amount: " + times);
			}
			checkUnTillBuilt(times);
		} else if (TestEnvironment.PRODUCT_UBL.contains(comp.getCoords().toString(""))) {
			checkUBLBuilt(times);
		} else if (TestEnvironment.PRODUCT_UNTILLDB.contains(comp.getCoords().toString(""))) {
			checkUnTillDbBuilt(times);
		} else {
			fail("unexpected coords: " + comp.getCoords());
		}
	}
	
	protected void forkAndBuild(Component comp, int times) {
		fork(comp, times);
		build(comp, times);
	}
}
