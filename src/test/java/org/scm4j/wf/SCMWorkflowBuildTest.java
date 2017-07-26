package org.scm4j.wf;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branchstatus.DevelopBranch;
import org.scm4j.wf.branchstatus.ReleaseBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.scmactions.ReleaseReason;
import org.scm4j.wf.scmactions.SCMActionBuild;
import org.scm4j.wf.scmactions.SCMActionForkReleaseBranch;
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
	public void testBuildUBLAndUnTillDb() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		
		// fork unTillDb release
		action.execute(new NullProgress()); 
		
		// fork UBL
		action = wf.getProductionReleaseAction(UBL);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {;
			action.execute(progress);
		}
		Expectations exp = new Expectations();
		exp.put(UBL, SCMActionForkReleaseBranch.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_DEPENDENCIES);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		assertTrue(TestBuilder.getBuilders().isEmpty());
		
		// build UBL and unTillDb
		action = wf.getProductionReleaseAction(UBL);
		exp = new Expectations();
		exp.put(UBL, SCMActionBuild.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_DEPENDENCIES);
		exp.put(UBL, "targetVersion", rbUBL.getVersion());
		exp.put(UNTILLDB, SCMActionBuild.class);
		exp.put(UNTILLDB, "reason", ReleaseReason.NEW_FEATURES);
		exp.put(UNTILLDB, "targetVersion", rbUnTillDb.getVersion());
		checkChildActionsTypes(action, exp);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		assertNotNull(TestBuilder.getBuilders());
		assertTrue(TestBuilder.getBuilders().size() == 2);
		assertNotNull(TestBuilder.getBuilders().get(UNTILLDB));
		assertNotNull(TestBuilder.getBuilders().get(UBL));
	}
	
	@Test
	public void testBuildUnTillDb() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		
		// fork unTillDb release
		action.execute(new NullProgress()); 
		
		action = wf.getProductionReleaseAction(UNTILLDB);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, SCMActionBuild.class);
		exp.put(UNTILLDB, "reason", ReleaseReason.NEW_FEATURES);
		checkChildActionsTypes(action, exp);

		// build unTillDb
		assertTrue(TestBuilder.getBuilders().isEmpty());
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
//		assertNotNull(TestBuilder.getBuilders());
//		assertTrue(TestBuilder.getBuilders().size() == 1);
//		assertNotNull(TestBuilder.getBuilders().get(UNTILLDB));
//		
//		// fork unTill. Built unTillDb should be used. UBL and unTill must be forked due of new dependencies
//		action = wf.getProductionReleaseAction(UNTILL);
//		exp = new Expectations();
//		exp.put(UNTILLDB, ActionNone.class);
//		exp.put(UBL, SCMActionForkReleaseBranch.class);
//		exp.put(UBL, "reason", ReleaseReason.NEW_DEPENDENCIES);
//		exp.put(UNTILL, SCMActionForkReleaseBranch.class);
//		exp.put(UNTILL, "reason", ReleaseReason.NEW_DEPENDENCIES);
//		checkChildActionsTypes(action, exp);
//		
//		TestBuilder.setBuilders(new HashMap<String, TestBuilder>());
//		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
//			action.execute(progress);
//		}
//		assertNotNull(TestBuilder.getBuilders());
//		assertTrue(TestBuilder.getBuilders().size() == 0);
		
	}
	
	private void checkChildActionsTypes(IAction action, Expectations exp) {
		for (IAction nestedAction : action.getChildActions()) {
			checkChildActionsTypes(nestedAction, exp);
		}
		if (!exp.getProps().containsKey(action.getName())) {
			fail("unexpected action: " + action.getName());
		}
		Map<String, Object> props = exp.getProps().get(action.getName());
		Class<?> clazz = (Class<?>) props.get("class");
		if (!action.getClass().isAssignableFrom(clazz)) {
			fail("expected: " + clazz.toString() + ", actual: " + action.getClass().toString());
		}
		if (props.size() <= 1) {
			return; 
		}
		
		for (Method method : action.getClass().getMethods()) {
			if (!method.getName().startsWith("get")) {
				continue;
			}
			for (String propName : props.keySet()) {
				if (method.getName().toLowerCase().equals("get" + propName)) {
					Object propValue;
					try {
						propValue = method.invoke(action);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					if (!propValue.equals(props.get(propName))) {
						fail (String.format("%s: property %s failed: expected %s, actual %s", action.getName(), propName, propValue, props.get(propName)));
					}
					continue;
				}
			}
		}
	}
}
