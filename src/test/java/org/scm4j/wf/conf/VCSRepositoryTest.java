package org.scm4j.wf.conf;

import static org.junit.Assert.assertNotNull;

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
	
	@Test
	public void testToString() {
		assertNotNull(new VCSRepository("name", "url", null, null, null, null, null, null));
	}

}
