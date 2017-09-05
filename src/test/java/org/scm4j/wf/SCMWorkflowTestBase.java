package org.scm4j.wf;

import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.DevelopBranch;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.conf.DelayedTagsFile;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;

public class SCMWorkflowTestBase {
	protected TestEnvironment env;
	protected static final String UNTILL = TestEnvironment.PRODUCT_UNTILL;
	protected static final String UNTILLDB = TestEnvironment.PRODUCT_UNTILLDB;
	protected static final String UBL = TestEnvironment.PRODUCT_UBL;
	protected VCSRepositories repos;
	protected Component compUnTill;
	protected Component compUnTillDb;
	protected Component compUBL;
	protected DevelopBranch dbUnTill;
	protected DevelopBranch dbUnTillDb;
	protected DevelopBranch dbUBL;
	protected ReleaseBranch rbUnTillFixedVer;
	protected ReleaseBranch rbUnTillDbFixedVer;
	protected ReleaseBranch rbUBLFixedVer;

	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		repos = VCSRepositories.loadVCSRepositories();
		compUnTill = new Component(UNTILL, repos.getByCoords(UNTILL));
		compUnTillDb = new Component(UNTILLDB, repos.getByCoords(UNTILLDB));
		compUBL = new Component(UBL, repos.getByCoords(UBL));
		dbUnTill = new DevelopBranch(compUnTill);
		dbUnTillDb = new DevelopBranch(compUnTillDb);
		dbUBL = new DevelopBranch(compUBL);
		rbUnTillFixedVer = new ReleaseBranch(compUnTill, env.getUnTillVer(), repos);
		rbUnTillDbFixedVer = new ReleaseBranch(compUnTillDb, env.getUnTillDbVer(), repos);
		rbUBLFixedVer = new ReleaseBranch(compUBL, env.getUblVer(), repos);
		TestBuilder.setBuilders(new HashMap<String, TestBuilder>());
		new DelayedTagsFile().delete();
	}

	@After
	public void tearDown() throws Exception {
		if (env != null) {
			env.close();
		}
		TestBuilder.setBuilders(null);
	}
	
	protected void checkChildActionsTypes(IAction action, Expectations exp) {
		for (IAction nestedAction : action.getChildActions()) {
			checkChildActionsTypes(nestedAction, exp);
		}
		if (!exp.getProps().containsKey(action.getName())) {
			fail("unexpected action: " + action.getName());
		}
		Map<String, Object> props = exp.getProps().get(action.getName());
		Class<?> clazz = (Class<?>) props.get("class");
		if (!action.getClass().isAssignableFrom(clazz)) {
			fail(String.format("%s: expected: %s, actual: %s", action.getName(), clazz.toString(), action.getClass().toString()));
		}
		if (props.size() <= 1) {
			return; 
		}
		
		label1: for (String propName : props.keySet()) {
			for (Method method : action.getClass().getMethods()) {
				if (method.getName().toLowerCase().equals("get" + propName)) {
					Object propValue;
					try {
						propValue = method.invoke(action);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					if (!propValue.equals(props.get(propName))) {
						fail(String.format("%s: property %s failed: expected %s, actual %s", action.getName(), propName,
								propValue, props.get(propName)));
					}
					continue label1;
				}
			}
			fail(String.format("%s: property %s is not declared", action.getName(), propName));
		}
	}

}
