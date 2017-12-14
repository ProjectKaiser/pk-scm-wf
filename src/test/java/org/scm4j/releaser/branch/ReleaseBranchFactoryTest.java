package org.scm4j.releaser.branch;

import static org.junit.Assert.assertThat;
import static org.scm4j.releaser.UtilityClassMatcher.isUtilityClass;

import org.junit.Test;

public class ReleaseBranchFactoryTest {
	
	@Test
	public void testIsUtilityClass() {
		assertThat(ReleaseBranchFactory.class, isUtilityClass());
	}
}
