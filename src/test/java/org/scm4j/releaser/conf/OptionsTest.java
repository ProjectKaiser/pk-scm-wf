package org.scm4j.releaser.conf;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.scm4j.releaser.UtilityClassMatcher.isUtilityClass;

public class OptionsTest {

	@Test
	public void testIsUtilityClass() {
		assertThat(Options.class, isUtilityClass());
	}
}