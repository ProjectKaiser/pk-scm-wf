package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BuildStatusTest {
	
	@Test
	public void testOrder() {
		assertEquals(0, BuildStatus.FORK.ordinal());
		assertEquals(1, BuildStatus.LOCK.ordinal());
		assertEquals(2, BuildStatus.BUILD_MDEPS.ordinal());
		assertEquals(3, BuildStatus.ACTUALIZE_PATCHES.ordinal());
		assertEquals(4, BuildStatus.BUILD.ordinal());
		assertEquals(5, BuildStatus.DONE.ordinal());
		assertEquals(6, BuildStatus.ERROR.ordinal());
	}
}
