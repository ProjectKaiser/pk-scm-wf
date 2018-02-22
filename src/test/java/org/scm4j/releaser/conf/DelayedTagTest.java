package org.scm4j.releaser.conf;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class DelayedTagTest {
	@Test
	public void testEqualsAndHashcode() {
		EqualsVerifier
				.forClass(DelayedTag.class)
				.usingGetClass()
				.verify();
	}
}