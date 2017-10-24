package org.scm4j.releaser.conf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

public class URLContentLoaderTest {
	
	private static final String TEST_URL = "c:/command.com";

	@Test
	public void testNullContent() throws IOException {
		assertNull(new URLContentLoader().getContentFromUrl(null));
		assertNull(new URLContentLoader().getContentFromUrls(null));
	}
	
	@Test
	public void testDefaultProtocol() {
		URLContentLoader cl = new URLContentLoader();
		assertEquals(URLContentLoader.DEFAULT_PROTOCOL + TEST_URL, cl.getWithDefaultProtocol(TEST_URL));
		assertEquals("file:///" + TEST_URL, cl.getWithDefaultProtocol("file:///" + TEST_URL));
		assertEquals("http://" + TEST_URL, cl.getWithDefaultProtocol("http://" + TEST_URL));
		assertEquals("https://" + TEST_URL, cl.getWithDefaultProtocol("https://" + TEST_URL));
	}
}
