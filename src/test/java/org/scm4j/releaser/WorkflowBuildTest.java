package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.exceptions.EBuildOnNotForkedRelease;
import org.scm4j.releaser.exceptions.ENoBuilder;
import org.yaml.snakeyaml.Yaml;
public class WorkflowBuildTest extends WorkflowTestBase {
	
	@Test
	public void testBuildAllAndTestIGNOREDDev() throws Exception {
		forkAndBuild(compUnTill);
		
		// check nothing happens next time
		IAction action = execAndGetActionBuild(compUnTill);
		assertActionDoesNothing(action, compUnTill);
		checkUnTillBuilt();

		// test IGNORED dev branch state
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(),
				LogTag.SCM_IGNORE + " ignored feature commit added");
		action = execAndGetActionBuild(compUnTill);
		assertActionDoesNothing(action);
	}

	@Test
	public void testBuildRootIfNestedIsBuiltAlready() throws Exception {
		forkAndBuild(compUnTillDb);
		
		// fork UBL
		IAction action = execAndGetActionTreeFork(compUBL);
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
		IAction action = execAndGetActionTreeFork(compUnTillDb);
		assertActionDoesFork(action, compUnTillDb);
		checkUnTillDbForked();
		
		// fork UBL
		action = execAndGetActionTreeFork(compUBL);
		assertActionDoesFork(action, compUBL);
		assertActionDoesNothing(action, BuildStatus.BUILD, null, compUnTillDb);
		checkUBLForked();
		
		assertTrue(TestBuilder.getBuilders().isEmpty());
		
		// build UBL and unTillDb
		action = execAndGetActionBuild(compUBL);
		assertActionDoesBuild(action, compUnTillDb);
		assertActionDoesBuildBuild(action, compUBL, BuildStatus.BUILD_MDEPS);
		checkUBLBuilt();
	}
	
	@Test
	public void testBuildSingleComponentTwice() throws Exception {
		forkAndBuild(compUnTillDb);
		
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature commit added");

		forkAndBuild(compUnTillDb, 2);
	}
	
	@Test
	public void testSkipBuildsOnFORKActionKind() throws Exception {
		fork(compUnTill);

		// try to build with FORK target action kind. All builds should be skipped
		IAction action = execAndGetActionTreeFork(compUnTill);
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
		Map<String, ?> content = (Map<String, String>) yaml.load(FileUtils.readFileToString(env.getReposFile(), StandardCharsets.UTF_8));
		((Map<String, ?>) content.get("eu.untill:(.*)")).remove("releaseCommand");
		FileUtils.writeStringToFile(env.getReposFile(), yaml.dumpAsMap(content), StandardCharsets.UTF_8);
		
		try {
			forkAndBuild(compUnTillDb);
			fail();
		} catch (ENoBuilder e) {
		}
	}
}