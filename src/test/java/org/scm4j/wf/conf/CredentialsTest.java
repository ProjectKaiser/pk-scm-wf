package org.scm4j.wf.conf;

import static org.junit.Assert.*;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class CredentialsTest {
	
	private static final String PWD = "pwd";
	private static final String NAME = "name";

	@Test
	public void testEqualsAndHashCode() {
		EqualsVerifier
				.forClass(Credentials.class)
				.withOnlyTheseFields("name")
				.usingGetClass()
				.verify();
	}
	
	@Test
	public void testCredentials() {
		Credentials creds = new Credentials(NAME, PWD, true);
		assertEquals(NAME, creds.getName());
		assertEquals(PWD, creds.getPassword());
		assertTrue(creds.getIsDefault());
	}
	
	@Test
	public void testToString() {
		Credentials creds = new Credentials(NAME, PWD, true);
		assertTrue(creds.toString().contains(NAME));
		assertFalse(creds.toString().contains(PWD));
	}

}
