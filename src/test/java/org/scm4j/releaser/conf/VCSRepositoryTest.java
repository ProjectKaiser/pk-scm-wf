package org.scm4j.releaser.conf;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class VCSRepositoryTest {
	
	@Test
	public void testEqualsAndHashCode() {
		EqualsVerifier
				.forClass(VCSRepository.class)
				.withOnlyTheseFields("url")
				.usingGetClass()
				.verify();
	}
}
