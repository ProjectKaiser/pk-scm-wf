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

	protected void checkCompBuilt(int times, Component comp) {
		if (comp.getName().equals(compUnTill.getName())) {
			checkCompBuilt(times, comp, env.getUnTillVCS(), repoUnTill, env.getUnTillVer());
		} else if (comp.getName().equals(compUBL.getName())) {
			checkCompBuilt(times, comp, env.getUblVCS(), repoUBL, env.getUblVer());
		} else if (comp.getName().equals(compUnTillDb.getName())) {
			checkCompBuilt(times, comp, env.getUnTillDbVCS(), repoUnTillDb, env.getUnTillDbVer());
		}
	}

	private void checkCompBuilt(int times, Component comp, IVCS vcs, VCSRepository repo, Version initialVer) {
		checkCompForked(times, comp, initialVer, repo);
		Version latestVersion = getCrbVersion(comp);

		assertNotNull(TestBuilder.getBuilders().get(comp.getName()));

		assertTrue(Utils.getBuildDir(repo, latestVersion).exists());

		// check tags
		List<VCSTag> tags = vcs.getTags();
		assertEquals(times, tags.size());

		// check has tags for each built version
		Version expectedCompReleaseVer = initialVer.toReleaseZeroPatch().toPreviousMinor();
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
		checkCompBuilt(times, compUnTillDb);
	}

	protected void checkUBLBuilt(int times) {
		checkUnTillDbBuilt(times);
		checkCompBuilt(times, compUBL);
		checkUBLMDepsVersions(times);
	}

	public void checkUnTillBuilt(int times) {
		checkUBLBuilt(times);
		checkCompBuilt(times, compUnTill);
		checkUnTillMDepsVersions(times);
	}

	protected void checkUBLMDepsVersions(int times) {
		Version latestVersion = getCrbVersion(compUBL);
		List<Component> ublReleaseMDeps = ReleaseBranchFactory.getMDepsRelease(
				Utils.getReleaseBranchName(repoUBL, latestVersion), repoUBL);
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		checkCompVersions(times, ublReleaseMDeps.get(0).getVersion(), env.getUnTillDbVer(), repoUnTillDb);
	}

	protected void checkUnTillMDepsVersions(int times) {
		Version latestVersion = getCrbVersion(compUnTill);
		List<Component> untillReleaseMDeps = ReleaseBranchFactory.getMDepsRelease(
				Utils.getReleaseBranchName(repoUnTill, latestVersion), repoUnTill);
		assertTrue(untillReleaseMDeps.size() == 2);
		assertEquals(compUnTillDb.getName(), untillReleaseMDeps.get(1).getName());
		checkCompVersions(times, untillReleaseMDeps.get(1).getVersion(), env.getUnTillDbVer(), repoUnTillDb);

		assertEquals(compUBL.getName(), untillReleaseMDeps.get(0).getName());
		checkCompVersions(times, untillReleaseMDeps.get(0).getVersion(), env.getUblVer(), repoUBL);
	}

	private void checkCompVersions(int times, Version actualVersion, Version initialVersion, VCSRepository repo) {
		Version expectedVer = initialVersion.toReleaseZeroPatch().toPreviousMinor();
		for (int i = 0; i < times; i++) {
			expectedVer = expectedVer.toNextMinor();
		}
		assertEquals(expectedVer, actualVersion.toReleaseZeroPatch());
		Version expectedDevVer = expectedVer.toNextMinor().setPatch(initialVersion.getPatch()).toSnapshot();
		assertEquals(expectedDevVer, Utils.getDevVersion(repo));
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

	protected void checkCompForked(int times, Component comp) {
		if (comp.getName().equals(compUnTill.getName())) {
			checkCompForked(times, comp, env.getUnTillVer(), repoUnTill);
		} else if (comp.getName().equals(compUBL.getName())) {
			checkCompForked(times, comp, env.getUblVer(), repoUBL);
		} else if (comp.getName().equals(compUnTillDb.getName())) {
			checkCompForked(times, comp, env.getUnTillDbVer(), repoUnTillDb);
		}
	}

	private void checkCompForked(int times, Component comp, Version initialVer, VCSRepository repo) {
		Version latestVersion = getCrbVersion(comp);
		assertTrue(repo.getVCS().getBranches(repo.getReleaseBranchPrefix()).contains(
				Utils.getReleaseBranchName(repo, latestVersion)));
		checkCompVersions(times, latestVersion, initialVer, repo);
	}

	public void checkUBLForked(int times) {
		checkUnTillDbForked(times);
		checkCompForked(times, compUBL);
		checkUBLMDepsVersions(times);
	}
	
	public void checkUnTillDbForked(int times) {
		checkCompForked(times, compUnTillDb);
	}

	public void checkUnTillDbForked() {
		checkUnTillDbForked(1);
	}

	public void checkUnTillOnlyForked(int times) {
		checkCompForked(times, compUnTill);
		Version latestVersion = getCrbVersion(compUnTill);
		checkCompVersions(times, latestVersion, env.getUnTillVer(), repoUnTill);
		checkUnTillMDepsVersions(times - 1);
	}

	public void checkUnTillForked() {
		checkUBLForked();
		checkCompForked(1, compUnTill);
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
