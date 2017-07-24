package org.scm4j.wf;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branchstatus.DevelopBranch;
import org.scm4j.wf.branchstatus.ReleaseBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.scmactions.SCMActionBuild;

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
	}

	@After
	public void tearDown() throws Exception {
		if (env != null) {
			env.close();
		}
	}
	
	@Test
	public void testBuildUnTilDb() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		// fork releases
		action.execute(new NullProgress()); 
		
		action = wf.getProductionReleaseAction(UNTILLDB);
		Map<String, Class<?>> expected = new HashMap<>();
		expected.put(UNTILLDB, SCMActionBuild.class);
		expected.put(UBL, SCMActionBuild.class);
		expected.put(UNTILLDB, SCMActionBuild.class);
		checkChildActionsTypes(action, expected);

		// build all
		action.execute(new ProgressConsole(UNTILL, ">>> ", "<<< "));
		
	}
	
	private void checkChildActionsTypes(IAction action, Map<String, Class<?>> expected) {
		for (IAction nestedAction : action.getChildActions()) {
			checkChildActionsTypes(nestedAction, expected);
		}

		assertTrue(action.getClass().isAssignableFrom(expected.get(action.getName())));
	}

}
