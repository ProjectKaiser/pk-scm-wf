package org.scm4j.releaser.branch;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.scm4j.releaser.UtilityClassMatcher.isUtilityClass;

public class ReleaseBranchFactoryTest {
	
	@Test
	public void testIsUtilityClass() {
		assertThat(ReleaseBranchFactory.class, isUtilityClass());
	}
}
