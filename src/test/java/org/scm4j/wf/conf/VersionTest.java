package org.scm4j.wf.conf;

import static org.junit.Assert.*;

import org.junit.Test;

public class VersionTest {
	
	@Test
	public void testMinor() {
		assertEquals(new Version("11.21.31.41").getMinor(), "31");
		assertEquals(new Version("11.21.31").getMinor(), "21");
		assertEquals(new Version("11").getMinor(), "11");
		assertEquals(new Version("1").getMinor(), "1");
		assertEquals(new Version("").getMinor(), "");
	}
	
	@Test
	public void testToString() {
		assertEquals(new Version("11.21.31.41").toString(), "11.21.31.41");
		assertEquals(new Version("11.21.31.41-SNAPSHOT").toString(), "11.21.31.41-SNAPSHOT");
		assertEquals(new Version("11.21.31-SNAPSHOT").toString(), "11.21.31-SNAPSHOT");
		assertEquals(new Version("11.21-SNAPSHOT").toString(), "11.21-SNAPSHOT");
		assertEquals(new Version("11-SNAPSHOT").toString(), "0.11.0-SNAPSHOT");
		assertEquals(new Version("").toString(), "");
		
		assertEquals(new Version("11.21.31.41-SNAPSHOT").toReleaseString(), "11.21.31.41");
	}
	
	@Test
	public void testSnapshot() {
		assertEquals(new Version("11.21.31.41").getSnapshot(), "");
		assertEquals(new Version("11.21.31.41-SNAPSHOT").getSnapshot(), "-SNAPSHOT");
		assertEquals(new Version("11.21.31.41-jkhkjhk").getSnapshot(), "");
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
}	
