package org.scm4j.wf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.scmactions.ReleaseReason;
import org.scm4j.wf.scmactions.SCMActionBuild;
import org.scm4j.wf.scmactions.SCMActionForkReleaseBranch;

public class SCMWorkflowBuildTest extends SCMWorkflowTestBase {
	
	@Test
	public void testBuildUBLAfterUnillDb() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		
		// fork unTillDb 
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// build unTillDb
		action = wf.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// fork UBL
		TestBuilder.getBuilders().clear();
		action = wf.getProductionReleaseAction(UBL);
		Expectations exp = new Expectations();
		exp.put(UBL, SCMActionForkReleaseBranch.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_DEPENDENCIES);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {;
			action.execute(progress);
		}
		assertTrue(TestBuilder.getBuilders().isEmpty());
		
		// build UBL
		action = wf.getProductionReleaseAction(UBL);
		exp = new Expectations();
		exp.put(UBL, SCMActionBuild.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_DEPENDENCIES);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {;
			action.execute(progress);
		} 
		assertFalse(TestBuilder.getBuilders().isEmpty());
		assertTrue(TestBuilder.getBuilders().size() == 1);
		
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
		exp.put(UNTILLDB, SCMActionBuild.class);
		exp.put(UNTILLDB, "reason", ReleaseReason.NEW_FEATURES);
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
	
	
}
