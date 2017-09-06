package org.scm4j.wf;

import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.scmactions.ReleaseReason;
import org.scm4j.wf.scmactions.SCMActionBuild;
import org.scm4j.wf.scmactions.SCMActionFork;

import java.util.List;

import static org.junit.Assert.*;

public class WorkflowBuildTest extends WorkflowTestBase {

	private void checkUnTillDbForked() {
		ReleaseBranch newUnTillDbRB = new ReleaseBranch(compUnTillDb);
		// check branches
		assertTrue(env.getUnTillDbVCS().getBranches("").contains(newUnTillDbRB.getName()));

		// check versions. Trunk is minor+1, Release is without -SNAPSHOT
		Version verTrunk = dbUnTillDb.getVersion();

		Version verRelease = newUnTillDbRB.getCurrentVersion();
		assertEquals(env.getUnTillDbVer().toNextMinor(), verTrunk);
		assertEquals(env.getUnTillDbVer().toRelease(), verRelease);
	}
	
	@Test
	public void testBuildRootIfNestedIsBuiltAlready() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		
		// fork unTillDb 
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		checkUnTillDbForked();
		
		// build unTillDb
		action = wf.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// fork UBL
		// simulate BRANCHED dev branch status
		env.generateContent(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "test file", "test content", LogTag.SCM_VER);
		TestBuilder.getBuilders().clear();
		action = wf.getProductionReleaseAction(UBL);
		Expectations exp = new Expectations();
		exp.put(UBL, SCMActionFork.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_DEPENDENCIES);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		assertTrue(TestBuilder.getBuilders().isEmpty());
		
		// build UBL
		action = wf.getProductionReleaseAction(UBL);
		exp = new Expectations();
		exp.put(UBL, SCMActionBuild.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_FEATURES);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		} 
		assertFalse(TestBuilder.getBuilders().isEmpty());
		assertTrue(TestBuilder.getBuilders().size() == 1);
		
	}



	@Test
	public void testBuildUBLAndUnTillDb() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		
		// fork unTillDb
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, SCMActionFork.class);
		exp.put(UNTILLDB, "reason", ReleaseReason.NEW_FEATURES);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		// fork UBL
		// simulate BRANCHED dev branch status
		env.generateContent(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "test file", "test content", LogTag.SCM_VER);
		action = wf.getProductionReleaseAction(UBL);
		exp = new Expectations();
		exp.put(UBL, SCMActionFork.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_DEPENDENCIES);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
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
	public void testBuildSingleComponent() throws Exception {
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
		
		assertNotNull(TestBuilder.getBuilders());
		assertTrue(TestBuilder.getBuilders().size() == 1);
		assertNotNull(TestBuilder.getBuilders().get(UNTILLDB));
		
		// check versions
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		Version verRelease = rbUnTillDb.getCurrentVersion();
		assertEquals(env.getUnTillDbVer().toNextPatch().toReleaseString(), verRelease.toString());
		
		// check tags
		List<VCSTag> tags = env.getUnTillDbVCS().getTags();
		assertTrue(tags.size() == 1);
		VCSTag tag = tags.get(0);
		assertEquals(env.getUnTillDbVer().toReleaseString(), tag.getTagName());
		List<VCSCommit> commits = env.getUnTillDbVCS().getCommitsRange(rbUnTillDb.getName(), null, WalkDirection.DESC, 2);
		assertEquals(commits.get(1), tag.getRelatedCommit());
	}
	
	@Test
	public void testSkipBuildsIfParentUnforked() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		
		// fork unTillDb
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, SCMActionFork.class);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}

		ReleaseBranch rbUBL = new ReleaseBranch(compUBL);
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill);

		assertFalse(env.getUnTillVCS().getBranches("").contains(rbUnTill.getName()));
		assertTrue(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDb.getName()));
		assertFalse(env.getUblVCS().getBranches("").contains(rbUBL.getName()));
		
		// fork unTill. unTillDb build must be skipped
		// simulate UBL and unTill BRANCHED dev branch status
		env.generateContent(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "test file", "test content", LogTag.SCM_VER);
		env.generateContent(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "test file", "test content", LogTag.SCM_VER);
		action = wf.getProductionReleaseAction(UNTILL);
		exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UNTILL, SCMActionFork.class);
		exp.put(UNTILL, "reason", ReleaseReason.NEW_DEPENDENCIES);
		exp.put(UBL, SCMActionFork.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_DEPENDENCIES);
		checkChildActionsTypes(action, exp);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		assertTrue(env.getUnTillVCS().getBranches("").contains(rbUnTill.getName()));
		assertTrue(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDb.getName()));
		assertTrue(env.getUblVCS().getBranches("").contains(rbUBL.getName()));
	}
	
	@Test
	public void testBuildPatchOnPreviousRelease() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();
		
		// fork unTillDb 2.59
		IAction action = wf.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// build unTillDb 2.59.1
		action = wf.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// fork new unTillDb Release 2.60
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		action = wf.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// build new unTillDbRelease 2.60.1
		action = wf.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		assertEquals(env.getUnTillDbVer().toNextMinor().toRelease(), new ReleaseBranch(compUnTillDb).getVersion());
		
		//check desired version is selected
		Component comp = new Component(UNTILLDB + ":2.59.1");
		ReleaseBranch rb = new ReleaseBranch(comp);
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toPreviousMinor().toRelease(), rb.getVersion());
		
		// add feature for 2.59.2
		env.generateFeatureCommit(env.getUnTillDbVCS(), rb.getName(), "2.59.2 feature added");
		
		// build new unTIllDb patch
		action = wf.getProductionReleaseAction(comp);
		action.execute(new NullProgress());
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toPreviousMinor().toNextPatch().toRelease(), new ReleaseBranch(comp).getVersion());
	}
	
	// TODO: add actualize mdeps test if mdep dev version is not -SNAPSHOT, e.g. 123.3 or master-SNAPSHOT 
}