package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranchCurrent;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.exceptions.EBuildOnNotForkedRelease;
import org.scm4j.releaser.exceptions.ENoBuilder;
import org.yaml.snakeyaml.Yaml;
public class WorkflowBuildTest extends WorkflowTestBase {
	
	@Test
	public void testBuildAfterForkInParts() throws Exception {
		// fork unTillDb
		IAction action = execAndGetActionFork(compUnTillDb);
		assertActionDoesFork(action, compUnTillDb);
		checkUnTillDbForked();

		// fork UBL
		action = execAndGetActionFork(compUBL);
		assertActionDoesFork(action, compUBL);
		assertActionDoesNothing(action, BuildStatus.BUILD, null, compUnTillDb);
		checkUBLForked();

		// build UBL and unTillDb
		action = execAndGetActionBuild(compUBL);
		assertActionDoesBuild(action, compUnTillDb);
		assertActionDoesBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
		checkUBLBuilt();
	}
	
	@Test
	public void testBuildSingleComponentTwice() throws Exception {
		forkAndBuild(compUnTillDb);

		env.generateFeatureCommit(env.getUnTillDbVCS(), repoUnTillDb.getDevelopBranch(), "feature commit added");

		forkAndBuild(compUnTillDb, 2);
	}
	
	@Test
	public void testBuildOnNotForkedReleaseException() {
		try {
			execAndGetActionBuild(compUnTill);
			fail();
		} catch (EBuildOnNotForkedRelease e) {
			assertEquals(compUnTillDb, e.getComp());
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testNoBuilderException() throws Exception {
		// simulate no builder
		Yaml yaml = new Yaml();
		Map<String, ?> content = (Map<String, String>) yaml.load(FileUtils.readFileToString(env.getCcFile(), StandardCharsets.UTF_8));
		((Map<String, ?>) content.get("eu.untill:(.*)")).remove("releaseCommand");
		FileUtils.writeStringToFile(env.getCcFile(), yaml.dumpAsMap(content), StandardCharsets.UTF_8);
		repoFactory = env.getRepoFactory();
		
		try {
			forkAndBuild(compUnTillDb);
			fail();
		} catch (ENoBuilder e) {
		}
	}
	
	@Test
	public void testActualizePatches() {
		fork(compUnTill);
		build(compUnTillDb);
		
		// add feature to existing unTillDb release
		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), crb.getName(), "patch feature added");

		// build unTillDb patch
		Component compUnTillDbPatch = new Component(UNTILLDB + ":" + env.getUnTillDbVer().toRelease());
		execAndGetActionBuild(compUnTillDbPatch);
		
		// UBL should actualize its mdeps
		IAction action = execAndGetActionBuild(compUnTill);
		assertActionDoesBuild(action, compUnTill, BuildStatus.BUILD_MDEPS);
		assertActionDoesBuild(action, compUBL, BuildStatus.ACTUALIZE_PATCHES);
		assertActionDoesNothing(action, compUnTillDb);
		
		// check unTill actualized unTillDb version
		checkUnTillMDepsVersions(1);

		// check UBL actualized unTillDb version
		checkUBLMDepsVersions(1);
	}
	
	@Test
	public void testNoActualizePatchesIfHasNewReleases() {
		forkAndBuild(compUnTill);
		
		// release next 2.60 unTillDb minor
		env.generateFeatureCommit(env.getUnTillDbVCS(), repoUnTillDb.getDevelopBranch(), "feature added");
		forkAndBuild(compUnTillDb, 2);
		
		IAction action = execAndGetActionBuild(compUnTill.clone(getCrbVersion(compUnTill)));
		assertActionDoesNothing(action, compUnTill);
		assertActionDoesNothing(action, compUBL);
		assertActionDoesNothing(action, compUnTillDb);
	}

	@Test
	public void testLockMDeps() {
		fork(compUBL);

		// simulate mdeps not locked
		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUBL);
		MDepsFile mdf = new MDepsFile(env.getUblVCS().getFileContent(crb.getName(), Constants.MDEPS_FILE_NAME, null));
		mdf.replaceMDep(mdf.getMDeps().get(0).clone(""));
		env.getUblVCS().setFileContent(crb.getName(), Constants.MDEPS_FILE_NAME, mdf.toFileContent(), "mdeps not locked");

		// UBL should lock its mdeps
		IAction action = execAndGetActionBuild(compUBL);
		assertActionDoesBuild(action, compUBL, BuildStatus.LOCK);

		// check UBL mdeps locked
		checkUBLMDepsVersions(1);
	}
	
	@Test
	public void testBuiltRootIfNestedBuiltAndModified() {
		fork(compUnTill);
		build(compUnTillDb);
		
		// add feature to trunk and release branch
		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), crb.getName(), "patch feature added");
		env.generateFeatureCommit(env.getUnTillDbVCS(), null, "patch feature added");
		
		// unTill should be built using locked unTillDb version
		IAction action = execAndGetActionBuild(compUnTill);
		assertActionDoesBuild(action, compUnTill, BuildStatus.BUILD_MDEPS);
		assertActionDoesBuild(action, compUBL, BuildStatus.BUILD);
		assertActionDoesNothing(action, compUnTillDb);
		checkCompBuilt(1, compUnTill);
		checkCompBuilt(1, compUBL);
		crb = ReleaseBranchFactory.getCRB(repoUnTillDb);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), crb.getVersion());
	}
	
	@Test
	public void testMinorUpgradeIsOKOnMinor() {
		fork(compUnTill);
		build(compUnTillDb);
		// add feature to trunk and release branch
		env.generateFeatureCommit(env.getUnTillDbVCS(), null, "feature added");
		
		// release next unTillDb version
		forkAndBuild(compUnTillDb, 2);
		//execAndGetActionBuildDelayedTag(compUnTillDb);
		
		// expect no EMinorUpgradeDowngrade exception because areMDepsPatchesActualForMinor should be used for minor
		status(compUBL);
	}
	
	@Test
	public void testShouldRemoveFromCacheOnErrorsOnMinor() {
		ExtendedStatusBuilder esb = spy(new ExtendedStatusBuilder(repoFactory));
		CachedStatuses cache = spy(new CachedStatuses());
		RuntimeException testException = new RuntimeException("");
		ProgressConsole pc = new ProgressConsole();
		ConcurrentHashMap<Component, ExtendedStatus> subComponentsLocal = new ConcurrentHashMap<Component, ExtendedStatus>();
		Component versionedUBL = compUBL.clone("1.0");
		doThrow(testException).when(esb).recursiveGetAndCacheStatus(cache, pc, subComponentsLocal, compUnTillDb, false);
		
		try {
			esb.getAndCacheStatus(versionedUBL, cache, pc, false);
			fail();
		} catch (RuntimeException e) {
			verify(cache, atLeast(1)).remove(eq(repoUBL.getUrl()));
		}
	}
	
	@Test
	public void testShouldRemoveFromCacheOnErrorsOnPatch() {
		forkAndBuild(compUBL);
		ExtendedStatusBuilder esb = spy(new ExtendedStatusBuilder(repoFactory));
		CachedStatuses cache = spy(new CachedStatuses());
		RuntimeException testException = new RuntimeException("");
		ProgressConsole pc = new ProgressConsole();
		ConcurrentHashMap<Component, ExtendedStatus> subComponentsLocal = new ConcurrentHashMap<Component, ExtendedStatus>();
		Component versionedUBL = compUBL.clone(getCrbVersion(compUBL));
		Component versionedUnTillDb = new Component("eu.untill:unTillDb:2.59.0#comment 3");
		doThrow(testException).when(esb).recursiveGetAndCacheStatus(cache, pc, subComponentsLocal, versionedUnTillDb, true);
		
		try {
			esb.getAndCacheStatus(versionedUBL, cache, pc, true);
			fail();
		} catch (RuntimeException e) {
			verify(cache, atLeast(1)).remove(eq(repoUBL.getUrl()));
		}
	}
	
	@Test
	public void testShouldRemoveFromCacheOnErrorsIsNeedToFork() {
		ExtendedStatusBuilder esb = spy(new ExtendedStatusBuilder(repoFactory));
		CachedStatuses cache = spy(new CachedStatuses());
		RuntimeException testException = new RuntimeException("");
		ProgressConsole pc = new ProgressConsole();
		ConcurrentHashMap<Component, ExtendedStatus> subComponentsLocal = new ConcurrentHashMap<Component, ExtendedStatus>();
		doThrow(testException).when(esb).recursiveGetAndCacheStatus(cache, pc, subComponentsLocal, compUnTillDb, false);
		
		try {
			esb.getAndCacheStatus(compUBL, cache, pc, false);
			fail();
		} catch (RuntimeException e) {
			verify(cache, atLeast(1)).remove(eq(repoUBL.getUrl()));
		}
	}
}