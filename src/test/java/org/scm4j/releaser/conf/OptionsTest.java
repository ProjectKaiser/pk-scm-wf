package org.scm4j.releaser.conf;

import static org.junit.Assert.assertThat;
import static com.almondtools.conmatch.conventions.UtilityClassMatcher.*;

import org.junit.Test;

public class OptionsTest {

	@Test
	public void testIsUtilityClass() {
		assertThat(Options.class, isUtilityClass());
	}
}