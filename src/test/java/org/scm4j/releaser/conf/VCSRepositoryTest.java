package org.scm4j.releaser.conf;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.scm4j.releaser.conf.VCSRepository;

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
	
	@Test
	public void testToString() {
		assertNotNull(new VCSRepository("name", "url", null, null, null, null, null, null));
	}

}
