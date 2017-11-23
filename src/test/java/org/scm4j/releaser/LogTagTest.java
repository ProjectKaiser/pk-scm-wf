package org.scm4j.releaser;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.scm4j.releaser.UtilityClassMatcher.isUtilityClass;

public class LogTagTest {

	@Test
	public void testIsUtilityClass() {
		assertThat(LogTag.class, isUtilityClass());
	}
}
