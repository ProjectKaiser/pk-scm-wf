package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.releaser.branch.ReleaseBranchFactory;

public class Coverage {

	@Test
	public void cover() {
		new Utils();
		new LogTag();
		new ReleaseBranchFactory();
	}
}
