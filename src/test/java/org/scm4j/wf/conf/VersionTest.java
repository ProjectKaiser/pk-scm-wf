package org.scm4j.wf.conf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertThat;

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
		assertEquals(new Version("11-SNAPSHOT").toString(), "11-SNAPSHOT");
		assertEquals(new Version("").toString(), "");
		assertEquals(new Version("1..1").toString(), "1..1");
	}
	
	@Test
	public void testToReleaseString() {
		assertEquals(new Version("11.21.31.41-SNAPSHOT").toReleaseString(), "11.21.31.41");
		assertEquals(new Version("11.21.31.41").toReleaseString(), "11.21.31.41");
		assertEquals(new Version("11.21.31").toReleaseString(), "11.21.31");
		assertEquals(new Version("11.21").toReleaseString(), "11.21");
		assertEquals(new Version("11-SNAPSHOT").toReleaseString(), "11");
		assertEquals(new Version("-SNAPSHOT").toReleaseString(), "-SNAPSHOT");
		
	}
	
	@Test
	public void testSnapshot() {
		assertEquals(new Version("11.21.31.41").getSnapshot(), "");
		assertEquals(new Version("11.21.31.41-SNAPSHOT").getSnapshot(), "-SNAPSHOT");
		assertEquals(new Version("11.21.31.41-jkhkjhk").getSnapshot(), "");
		assertEquals(new Version("-SNAPSHOT").getSnapshot(), "-SNAPSHOT");
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
			assertThat(version.toPreviousMinor(), null);
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
	public void testIsSnapshot() {
		assertTrue(new Version("11.12.13-SNAPSHOT").isSnapshot());
		assertFalse(new Version("11.12.13").isSnapshot());
		assertFalse(new Version("").isSnapshot());
		assertTrue(new Version("-SNAPSHOT").isSnapshot());
	}

	@Test
	public void testPatch() {
		assertEquals("13", new Version("11.12.13-SNAPSHOT").getPatch());
		assertEquals("dfgdfg", new Version("11.12.dfgdfg-SNAPSHOT").getPatch());
		assertEquals("14", new Version("11.12.13.14-SNAPSHOT").getPatch());
	}

	@Test
	public void testToNextPatch() {
		assertEquals("11.12.14-SNAPSHOT", new Version("11.12.13-SNAPSHOT").toNextPatch().toString());
		assertEquals("14", new Version("11.12.13-SNAPSHOT").toNextPatch().getPatch());
		assertEquals("11.12.14fgdfg-SNAPSHOT", new Version("11.12.14fgdfg-SNAPSHOT").toNextPatch().toString());
		assertEquals("13.1", new Version("13").toNextPatch().toString());
	}
	
	@Test
	public void testToPreviousPatch() {
		assertEquals("11.12.12-SNAPSHOT", new Version("11.12.13-SNAPSHOT").toPreviousPatch().toString());
		assertEquals("12", new Version("11.12.13-SNAPSHOT").toPreviousPatch().getPatch());
		assertEquals("11.12.12fgdfg-SNAPSHOT", new Version("11.12.12fgdfg-SNAPSHOT").toPreviousPatch().toString());
		assertEquals("13.0", new Version("13").toPreviousPatch().toString());
		assertEquals("13.14.fgdfgd", new Version("13.14.fgdfgd").toPreviousPatch().toString());
	}

	@Test
	public void testIsGreaterThan() {
		assertTrue(new Version("11.12.13.14-SNAPSHOT").isGreaterThan(new Version("11.12.13.13-SNAPSHOT")));
		assertFalse(new Version("11.12.13.14df-SNAPSHOT").isGreaterThan(new Version("11.12.13.13gh-SNAPSHOT")));
		assertFalse(new Version("11.12.13.14-SNAPSHOT").isGreaterThan(new Version("11.12.13.13gh-SNAPSHOT")));
		assertFalse(new Version("11.12.13.14dfg-SNAPSHOT").isGreaterThan(new Version("11.12.13.13-SNAPSHOT")));
		assertTrue(new Version("11.12.14-SNAPSHOT").isGreaterThan(new Version("11.12.13-SNAPSHOT")));
		assertTrue(new Version("11.12-SNAPSHOT").isGreaterThan(new Version("11.11-SNAPSHOT")));
		assertTrue(new Version("11-SNAPSHOT").isGreaterThan(new Version("10-SNAPSHOT")));
		
		assertFalse(new Version("").isGreaterThan(new Version("")));
		assertFalse(new Version("").isGreaterThan(new Version("11.12.13")));
		assertTrue(new Version("11.12.13").isGreaterThan(new Version("")));
		assertTrue(new Version("11.12.13").isGreaterThan(new Version("not semantic")));
		
		assertFalse(new Version("11.12.13-SNAPSHOT").isGreaterThan(new Version("11.12.14-SNAPSHOT")));
		assertFalse(new Version("11.12-SNAPSHOT").isGreaterThan(new Version("11.13-SNAPSHOT")));
		assertFalse(new Version("11-SNAPSHOT").isGreaterThan(new Version("12-SNAPSHOT")));
	}
	
	@Test
	public void testGetReleaseNoPatchString() {
		assertEquals("11.12.13", new Version("11.12.13.14-SNAPSHOT").getReleaseNoPatchString());
		assertEquals("11.12", new Version("11.12.13-SNAPSHOT").getReleaseNoPatchString());
		assertEquals("11", new Version("11.12-SNAPSHOT").getReleaseNoPatchString());
		assertEquals("11", new Version("11-SNAPSHOT").getReleaseNoPatchString());
	}
	
	@Test
	public void testToRelease() {
		assertEquals(new Version("11.21.31.41"), new Version("11.21.31.41-SNAPSHOT").toRelease());
		assertEquals(new Version("11.21.31.41"), new Version("11.21.31.41").toRelease());
		assertEquals(new Version("11.21.31"),  new Version("11.21.31").toRelease());
		assertEquals(new Version("11.21"), new Version("11.21").toRelease());
		assertEquals(new Version("11"), new Version("11-SNAPSHOT").toRelease());
		assertEquals(new Version("-SNAPSHOT"), new Version("-SNAPSHOT").toRelease());
	}
}
