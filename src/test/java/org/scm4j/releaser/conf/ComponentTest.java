package org.scm4j.releaser.conf;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class ComponentTest {

	@Test
	public void testEqualsAndHashCode() {
		EqualsVerifier
				.forClass(Component.class)
				.withOnlyTheseFields("coords")
				.usingGetClass()
				.verify();
	}

}