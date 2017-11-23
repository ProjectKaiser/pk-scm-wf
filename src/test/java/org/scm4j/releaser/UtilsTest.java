package org.scm4j.releaser;

import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.scm4j.releaser.UtilityClassMatcher.isUtilityClass;

public class UtilsTest {

	@Test
	public void testIsUtilityClass() {
		assertThat(Utils.class, isUtilityClass());
	}
}