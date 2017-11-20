package org.scm4j.releaser;

import static org.junit.Assert.assertThat;
import static com.almondtools.conmatch.conventions.UtilityClassMatcher.*;

import org.junit.Test;

public class LogTagTest {

	@Test
	public void testIsUtilityClass() {
		assertThat(LogTag.class, isUtilityClass());
	}
}
