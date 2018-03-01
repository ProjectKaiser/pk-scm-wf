package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.releaser.actions.IAction;

public class WorkflowForkAndBuildTest extends WorkflowTestBase {

	@Test
	public void testForkAndBuildAll() {
		forkAndBuild(compUnTill);

		// check nothing happens next time
		IAction action = execAndGetActionBuild(compUnTill);
		assertActionDoesNothing(action, compUnTill);
		checkUnTillBuilt(1);

		// test IGNORED dev branch state
		env.generateFeatureCommit(env.getUnTillDbVCS(), repoUnTillDb.getDevelopBranch(),
				Constants.SCM_IGNORE + " ignored feature commit added");
		action = execAndGetActionBuild(compUnTill);
		assertActionDoesNothing(action);
		checkUnTillBuilt(1);

		// test fork and build on new feature
		env.generateFeatureCommit(env.getUnTillDbVCS(), repoUnTillDb.getDevelopBranch(), "valuable feature commit added");
		forkAndBuild(compUnTill, 2);
	}

	@Test
	public void testForkAndBuildRootIfNestedIsBuilt() {
		forkAndBuild(compUBL);

		// next fork unTillDb
		env.generateFeatureCommit(env.getUnTillDbVCS(), repoUnTillDb.getDevelopBranch(), "feature added");
		forkAndBuild(compUnTillDb, 2);

		// UBL should be forked and built then
		IAction action  = execAndGetActionFork(compUBL);
		assertActionDoesFork(action, compUBL);
		assertActionDoesNothing(action, compUnTillDb);
		checkUBLForked(2);

		action  = execAndGetActionBuild(compUBL);
		assertActionDoesBuild(action, compUBL);
		assertActionDoesNothing(action, compUnTillDb);
		checkUBLForked(2);
	}

	@Test
	public void testForkAndBuildRootOnly() {
		forkAndBuild(compUnTill);

		env.generateFeatureCommit(env.getUnTillVCS(), repoUnTill.getDevelopBranch(), "feature added");

		// fork untill only
		IAction action = execAndGetActionFork(compUnTill);
		assertActionDoesFork(action, compUnTill);
		assertActionDoesNothing(action, compUnTillDb, compUBL);
		checkUBLForked(1);
		checkUnTillOnlyForked(2);

		// build untill only
		action = execAndGetActionBuild(compUnTill);
		assertActionDoesBuild(action, compUnTill);
		assertActionDoesNothing(action, compUnTillDb, compUBL);
		checkUBLBuilt(1);
		checkCompBuilt(2, compUnTill);
	}

}
