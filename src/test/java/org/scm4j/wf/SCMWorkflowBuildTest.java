package org.scm4j.wf;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branchstatus.DevelopBranch;
import org.scm4j.wf.branchstatus.ReleaseBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.scmactions.SCMActionBuild;
import org.scm4j.wf.scmactions.SCMActionUseLastReleaseVersion;

public class SCMWorkflowBuildTest {
	private TestEnvironment env;
	private static final String UNTILL = TestEnvironment.PRODUCT_UNTILL;
	private static final String UNTILLDB = TestEnvironment.PRODUCT_UNTILLDB;
	private static final String UBL = TestEnvironment.PRODUCT_UBL;
	private VCSRepositories repos;
	private Component compUnTill;
	private Component compUnTillDb;
	private Component compUBL;
	private DevelopBranch dbUnTill;
	private DevelopBranch dbUnTillDb;
	private DevelopBranch dbUBL;
	private ReleaseBranch rbUnTill;
	private ReleaseBranch rbUnTillDb;
	private ReleaseBranch rbUBL;
	
	
	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		repos = VCSRepositories.loadVCSRepositories();
		compUnTill = new Component(UNTILL, repos);
		compUnTillDb = new Component(UNTILLDB, repos);
		compUBL = new Component(UBL, repos);
		dbUnTill = new DevelopBranch(compUnTill);
		dbUnTillDb = new DevelopBranch(compUnTillDb);
		dbUBL = new DevelopBranch(compUBL);
		rbUnTill = new ReleaseBranch(compUnTill, repos);
		rbUnTillDb = new ReleaseBranch(compUnTillDb, repos);
		rbUBL = new ReleaseBranch(compUBL, repos);
		TestBuilder.setBuilders(new HashMap<String, TestBuilder>());
	}

	@After
	public void tearDown() throws Exception {
		if (env != null) {
			env.close();
		}
		TestBuilder.setBuilders(null);
	}
	
	@Test
	public void testBuildUnTilDb() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		
		// fork unTillDb release
		action.execute(new NullProgress()); 
		
		action = wf.getProductionReleaseAction(UNTILLDB);
		Map<String, Class<?>> expected = new HashMap<>();
		expected.put(UNTILLDB, SCMActionBuild.class);
		checkChildActionsTypes(action, expected);

		// build unTillDb
		assertTrue(TestBuilder.getBuilders().isEmpty());
		try (IProgress progress = new ProgressConsole(UNTILLDB, ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		assertNotNull(TestBuilder.getBuilders());
		assertNotNull(TestBuilder.getBuilders().get(UNTILLDB));
		
		// build unTill. Built unTillDb should be used. UBL and unTill must be released due of new dependencies
		action = wf.getProductionReleaseAction(UNTILL);
		expected.clear();
		expected.put(UNTILLDB, SCMActionUseLastReleaseVersion.class);
		expected.put(UBL, SCMActionBuild.class);
		expected.put(UNTILL, SCMActionBuild.class);
		checkChildActionsTypes(action, expected);
		
		TestBuilder.setBuilders(new HashMap<String, TestBuilder>());
		try (IProgress progress = new ProgressConsole(UNTILL, ">>> ", "<<< ")) {
			action.execute(progress);
		}
		assertNotNull(TestBuilder.getBuilders());
		assertTrue(TestBuilder.getBuilders().size() == 2);
		assertNull(TestBuilder.getBuilders().get(UNTILLDB));
		assertNotNull(TestBuilder.getBuilders().get(UNTILL));
		assertNotNull(TestBuilder.getBuilders().get(UBL));
	}
	
	private void checkChildActionsTypes(IAction action, Map<String, Class<?>> expected) {
		for (IAction nestedAction : action.getChildActions()) {
			checkChildActionsTypes(nestedAction, expected);
		}

		if (!action.getClass().isAssignableFrom(expected.get(action.getName()))) {
			fail (action.toString() + " check failed. Expected: " + expected.get(action.getName()) + ", actual: " + action.getClass());
		}
	}

}
