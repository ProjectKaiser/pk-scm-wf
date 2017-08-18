package org.scm4j.wf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.scm4j.wf.conf.Version;

import nl.jqno.equalsverifier.EqualsVerifier;

public class VersionTest {
	
	@Test
	public void testMinor() {
		assertEquals(new Version("11.21.31.41").getMinor(), "31");
		assertEquals(new Version("11.21.31").getMinor(), "21");
		assertEquals(new Version("11.21").getMinor(), "11");
		assertEquals(new Version("11").getMinor(), "11");
		assertEquals(new Version("1").getMinor(), "1");
		assertEquals(new Version("").getMinor(), "");
		assertEquals(new Version("-SNAPSHOT").getMinor(), "");
	}
	
	@Test
	public void testToString() {
		assertEquals(new Version("11.21.31.41").toString(), "11.21.31.41");
		assertEquals(new Version("11.21.31.41-SNAPSHOT").toString(), "11.21.31.41-SNAPSHOT");
		assertEquals(new Version("11.21.31-SNAPSHOT").toString(), "11.21.31-SNAPSHOT");
		assertEquals(new Version("11.21-SNAPSHOT").toString(), "11.21-SNAPSHOT");
		assertEquals(new Version("11-SNAPSHOT").toString(), "0.11.0-SNAPSHOT");
		assertEquals(new Version("").toString(), "");
		assertEquals(new Version("1..1").toString(), "1..1");
	}
	
	@Test
	public void testToReleaseString() {
		assertEquals(new Version("11.21.31.41-SNAPSHOT").toReleaseString(), "11.21.31.41");
		assertEquals(new Version("11.21.31.41").toReleaseString(), "11.21.31.41");
		assertEquals(new Version("11.21.31").toReleaseString(), "11.21.31");
		assertEquals(new Version("11.21").toReleaseString(), "11.21");
		assertEquals(new Version("11-SNAPSHOT").toReleaseString(), "0.11.0");
		try {
			assertEquals(new Version("-SNAPSHOT").toReleaseString(), "0..0");
		} catch (IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testSnapshot() {
		assertEquals(new Version("11.21.31.41").getSnapshot(), "");
		assertEquals(new Version("11.21.31.41-SNAPSHOT").getSnapshot(), "-SNAPSHOT");
		assertEquals(new Version("11.21.31.41-jkhkjhk").getSnapshot(), "");
		assertEquals(new Version("-SNAPSHOT").getSnapshot(), "-SNAPSHOT");
	}
	
	@Test
	public void testIncorrectVersion() {
		try {
			new Version("sdfdfgdd");
			fail();
		} catch (IllegalArgumentException e) {
		}
		try {
			new Version("sdfdfgdd.0");
			fail();
		} catch (IllegalArgumentException e) {
		}
		try {
			new Version("1.sdfdfgdd.0");
			fail();
		} catch (IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testMinorBumping() {
		assertEquals(new Version("11.21.31.41").toPreviousMinor().toReleaseString(), "11.21.30.41");
		assertEquals(new Version("11.21.31.41-SNAPSHOT").toPreviousMinor().toReleaseString(), "11.21.30.41");
		assertEquals(new Version("11.21.31.41").toNextMinor().toReleaseString(), "11.21.32.41");
		assertEquals(new Version("11.21.31.41-SNAPSHOT").toNextMinor().toReleaseString(), "11.21.32.41");
		assertEquals(new Version("11.21.31.41").toNextMinor().toReleaseString(), "11.21.32.41");
		assertEquals(new Version("11.21.31.41-SNAPSHOT").toNextMinor().toString(), "11.21.32.41-SNAPSHOT");
		Version version = new Version("");
		try {
			version.toNextMinor();
			fail();
		} catch (IllegalArgumentException e) {
		}
		try {
			version.toPreviousMinor();
			fail();
		} catch (IllegalArgumentException e) {
		}
		try {
			version.toNextMinor();
			fail();
		} catch (IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testEmpty() {
		assertTrue(new Version("").isEmpty());
		assertFalse(new Version("11.21.31.41").isEmpty());
	}
	
	@Test
	public void testEqualsAndHashcode() {
		EqualsVerifier
				.forClass(Version.class)
				.withOnlyTheseFields("verStr")
				.usingGetClass()
				.verify();
	}
	
	@Test
	public void testExactVersion() {
		assertTrue(new Version("11.12.13-SNAPSHOT").isExactVersion());
		assertTrue(new Version("11.12.13").isExactVersion());
		assertTrue(new Version("11.13").isExactVersion());
		assertTrue(new Version("11").isExactVersion());
		assertFalse(new Version("").isExactVersion());
		assertFalse(new Version("-SNAPSHOT").isExactVersion());
		
	}
}
