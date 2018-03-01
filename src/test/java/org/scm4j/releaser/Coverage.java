package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.conf.VCSType;

public class Coverage {

	@Test
	public void cover() {
		new Utils();
		new ReleaseBranchFactory();
		new VCSRepository("name", "url", null, null, null, null, null, null).toString();
		for (VCSType type : VCSType.values()) {
			Utils.getBuildTimeEnvVars(type,"", "", "");
		}
		ExtendedStatus.DUMMY.toString();
		new Constants();
	}
}
