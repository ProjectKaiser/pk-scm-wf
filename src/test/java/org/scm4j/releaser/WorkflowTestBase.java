package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.Option;
import org.scm4j.releaser.conf.Options;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

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

	@Before
	public void setUp() throws Exception {
		env = new TestEnvironment();
		env.generateTestEnvironment();
		compUnTill = new Component(UNTILL);
		compUnTillDb = new Component(UNTILLDB);
		compUBL = new Component(UBL);
		dbUnTill = new DevelopBranch(compUnTill);
		dbUnTillDb = new DevelopBranch(compUnTillDb);
		dbUBL = new DevelopBranch(compUBL);
		TestBuilder.setBuilders(new HashMap<String, TestBuilder>());
		new DelayedTagsFile().delete();
	}

	@After
	public void tearDown() throws Exception {
		if (env != null) {
			env.close();
		}
		TestBuilder.setBuilders(null);
		Options.setOptions(new ArrayList<Option>());
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
	
	public void checkUnTillDbBuilt(int times) {
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		assertNotNull(TestBuilder.getBuilders());
		assertNotNull(TestBuilder.getBuilders().get(UNTILLDB));
		
		assertTrue(rbUnTillDb.getBuildDir().exists());

		// check versions
		Version verRelease = rbUnTillDb.getVersion();
		Version expectedReleaseVer = env.getUnTillDbVer().toReleaseZeroPatch().toPreviousMinor().toNextPatch();
		for (int i = 0; i < times; i++) {
			expectedReleaseVer = expectedReleaseVer.toNextMinor();
		}
		assertEquals(expectedReleaseVer, verRelease);

		// check tags
		List<VCSCommit> commits = env.getUnTillDbVCS().getCommitsRange(rbUnTillDb.getName(), null, WalkDirection.DESC, 2);
		List<VCSTag> tags = env.getUnTillDbVCS().getTagsOnRevision(commits.get(1).getRevision());
		assertTrue(tags.size() == 1);
		VCSTag tag = tags.get(0);
		assertEquals(verRelease.toPreviousPatch().toString(), tag.getTagName());
	}
	
	public void checkUnTillDbBuilt() {
		checkUnTillDbBuilt(1);
	}

	public void checkUBLBuilt() {
		checkUnTillDbBuilt();
		ReleaseBranch rbUBL = new ReleaseBranch(compUBL);
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		
		assertTrue(rbUBL.getBuildDir().exists());
		
		// check UBL versions
		assertEquals(env.getUblVer().toNextMinor(), dbUBL.getVersion());
		assertEquals(env.getUblVer().toReleaseZeroPatch(), rbUBL.getVersion().toReleaseZeroPatch());

		// check unTillDb versions
		assertEquals(env.getUnTillDbVer().toNextMinor(), dbUnTillDb.getVersion());
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch(), rbUnTillDb.getVersion().toReleaseZeroPatch());

		// check UBL mDeps
		List<Component> ublReleaseMDeps = rbUBL.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch(), ublReleaseMDeps.get(0).getVersion().toReleaseZeroPatch());

		// check tags
		List<VCSTag> tags = env.getUblVCS().getTags();
		assertTrue(tags.size() == 1);
		VCSTag tag = tags.get(0);
		assertEquals(dbUBL.getVersion().toPreviousMinor().toReleaseZeroPatch().toString(), tag.getTagName());
		List<VCSCommit> commits = env.getUblVCS().getCommitsRange(rbUBL.getName(), null, WalkDirection.DESC, 2);
		assertEquals(commits.get(1), tag.getRelatedCommit());
	}


	public void checkUBLForked() {
		ReleaseBranch rbUBL = new ReleaseBranch(compUBL);
		// check branches
		assertTrue(env.getUblVCS().getBranches(compUBL.getVcsRepository().getReleaseBranchPrefix()).contains(rbUBL.getName()));

		// check versions
		Version verTrunk = dbUBL.getVersion();
		Version verRelease = rbUBL.getVersion();
		assertEquals(env.getUblVer().toNextMinor(),verTrunk);
		assertEquals(env.getUblVer().toReleaseZeroPatch(), verRelease);

		// check mDeps
		List<Component> ublReleaseMDeps = rbUBL.getMDeps();
		assertTrue(ublReleaseMDeps.size() == 1);
		assertEquals(compUnTillDb.getName(), ublReleaseMDeps.get(0).getName());
		// do not consider patch because unTillDb could be build already befor UBL fork so target patch is unknown
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toReleaseZeroPatch(), ublReleaseMDeps.get(0).getVersion().toReleaseZeroPatch()); 
	}
	
	public void checkUnTillDbForked(int times) {
		ReleaseBranch newUnTillDbrb = new ReleaseBranch(compUnTillDb);

		// check branches
		assertTrue(env.getUnTillDbVCS().getBranches(compUnTillDb.getVcsRepository().getReleaseBranchPrefix()).contains(newUnTillDbrb.getName()));

		// check versions.
		Version verTrunk = dbUnTillDb.getVersion();
		Version verRelease = newUnTillDbrb.getVersion();
		Version expectedTrunkVer = env.getUnTillDbVer();
		Version expectedReleaseVer = env.getUnTillDbVer().toReleaseZeroPatch().toPreviousMinor();
		for (int i = 0; i < times; i++) {
			expectedTrunkVer = expectedTrunkVer.toNextMinor();
			expectedReleaseVer = expectedReleaseVer.toNextMinor();
		}
		assertEquals(expectedTrunkVer, verTrunk);
		assertEquals(expectedReleaseVer, verRelease);
	}

	public void checkUnTillDbForked() {
		checkUnTillDbForked(1);
	}

	public void checkUnTillForked() {
		checkUnTillDbForked();
		checkUBLForked();

		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill);

		// check branches
		assertTrue(env.getUnTillVCS().getBranches(compUnTill.getVcsRepository().getReleaseBranchPrefix()).contains(rbUnTill.getName()));

		// check versions
		Version verTrunk = dbUnTill.getVersion();
		Version verRelease = rbUnTill.getVersion();
		assertEquals(env.getUnTillVer().toNextMinor(), verTrunk);
		assertEquals(env.getUnTillVer().toReleaseZeroPatch(), verRelease);

		// check mDeps
		List<Component> unTillReleaseMDeps = rbUnTill.getMDeps();
		assertTrue(unTillReleaseMDeps.size() == 2);
		for (Component unTillReleaseMDep : unTillReleaseMDeps) {
			if (unTillReleaseMDep.getName().equals(UBL)) {
				assertEquals(env.getUblVer().toReleaseZeroPatch(), unTillReleaseMDep.getVersion());
			} else if (unTillReleaseMDep.getName().equals(UNTILLDB)) {
				assertEquals(env.getUnTillDbVer().toReleaseZeroPatch(), unTillReleaseMDep.getVersion());
			} else {
				fail();
			}
		}

	}

	public void checkUnTillBuilt() {
		checkUBLBuilt();
		ReleaseBranch rbUnTill= new ReleaseBranch(compUnTill);
		
		assertTrue(rbUnTill.getBuildDir().exists());
		
		// check versions
		assertEquals(env.getUnTillVer().toNextMinor(), dbUnTill.getVersion());
		assertEquals(env.getUnTillVer().toReleaseZeroPatch(), rbUnTill.getVersion().toReleaseZeroPatch());

		// check mDeps
		List<Component> untillReleaseMDeps = rbUnTill.getMDeps();
		assertTrue(untillReleaseMDeps.size() == 2);
		assertEquals(compUnTillDb.getName(), untillReleaseMDeps.get(1).getName());
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch(), untillReleaseMDeps.get(1).getVersion().toReleaseZeroPatch());
		assertEquals(compUBL.getName(), untillReleaseMDeps.get(0).getName());
		assertEquals(env.getUblVer().toReleaseZeroPatch(), untillReleaseMDeps.get(0).getVersion().toReleaseZeroPatch());

		// check tags
		List<VCSTag> tags = env.getUnTillVCS().getTags();
		assertTrue(tags.size() == 1);
		VCSTag tag = tags.get(0);
		assertEquals(dbUnTill.getVersion().toPreviousMinor().toReleaseZeroPatch().toString(), tag.getTagName());
		List<VCSCommit> commits = env.getUnTillVCS().getCommitsRange(rbUnTill.getName(), null, WalkDirection.DESC, 2);
		assertEquals(commits.get(1), tag.getRelatedCommit());
	}
	
	protected IProgress getProgress(IAction action) {
		return new ProgressConsole(action.toString(), ">>> ", "<<< ");
	}
}
