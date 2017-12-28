package org.scm4j.releaser;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranchCurrent;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.builders.TestBuilder;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.exceptions.EBuildOnNotForkedRelease;
import org.scm4j.releaser.exceptions.ENoBuilder;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.Assert.*;
public class WorkflowBuildTest extends WorkflowTestBase {
	
	@Test
	public void testBuildAllAndTestIGNOREDDev() throws Exception {
		forkAndBuild(compUnTill);
		
		// check nothing happens next time
		IAction action = execAndGetActionBuild(compUnTill);
		assertActionDoesNothing(action, compUnTill);
		checkUnTillBuilt(1);

		// test IGNORED dev branch state
		env.generateFeatureCommit(env.getUnTillDbVCS(), repoUnTillDb.getDevelopBranch(),
				LogTag.SCM_IGNORE + " ignored feature commit added");
		action = execAndGetActionBuild(compUnTill);
		assertActionDoesNothing(action);
	}

	@Test
	public void testBuildRootIfNestedIsBuiltAlready() throws Exception {
		forkAndBuild(compUnTillDb);
		
		// fork UBL
		IAction action = execAndGetActionFork(compUBL);
		assertActionDoesFork(action, compUBL);
		assertActionDoesNothing(action, compUnTillDb);
		checkUBLForked();
		
		// build UBL
		action = execAndGetActionBuild(compUBL);
		assertActionDoesBuild(action, compUBL);
		assertActionDoesNothing(action, compUnTillDb);
		checkUBLBuilt();
	}

	@Test
	public void testBuildRootAndChildIfAllForkedAlready() throws Exception {
		// fork unTillDb
		IAction action = execAndGetActionFork(compUnTillDb);
		assertActionDoesFork(action, compUnTillDb);
		checkUnTillDbForked();
		
		// fork UBL
		action = execAndGetActionFork(compUBL);
		assertActionDoesFork(action, compUBL);
		assertActionDoesNothing(action, BuildStatus.BUILD, null, compUnTillDb);
		checkUBLForked();
		
		assertTrue(TestBuilder.getBuilders().isEmpty());
		
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
	public void testSkipBuildsOnFORKActionKind() throws Exception {
		fork(compUnTill);

		// try to build with FORK target action kind. All builds should be skipped
		IAction action = execAndGetActionFork(compUnTill);
		assertActionDoesNothing(action, BuildStatus.BUILD_MDEPS, null, compUnTill, compUBL);
		assertActionDoesNothing(action, BuildStatus.BUILD, null, compUnTillDb);
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
		crb = ReleaseBranchFactory.getCRB(repoUnTill);
		MDepsFile mdf = new MDepsFile(env.getUnTillVCS().getFileContent(crb.getName(), Utils.MDEPS_FILE_NAME, null));
		assertThat(mdf.getMDeps(), Matchers.hasItem(compUnTillDbPatch));
		
		// check UBL actualized unTillDb version
		crb = ReleaseBranchFactory.getCRB(repoUBL);
		mdf = new MDepsFile(env.getUblVCS().getFileContent(crb.getName(), Utils.MDEPS_FILE_NAME, null));
		assertThat(mdf.getMDeps(), Matchers.hasItem(compUnTillDbPatch));
	}
}