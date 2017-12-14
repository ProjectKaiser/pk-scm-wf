package org.scm4j.releaser;

import org.junit.Test;

public class ExtendedStatusBuilderTest extends WorkflowTestBase {
	
	@Test
	public void testCircularDependencyDetection() {
		env.getUnTillDbVCS().setFileContent(compUnTillDb.getVcsRepository().getDevelopBranch(), Utils.MDEPS_FILE_NAME, compUBL.toString(), "circular dependency added");
		getActionTreeBuild(compUnTill);
		
		
	}

}
