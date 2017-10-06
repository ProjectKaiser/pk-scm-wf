package org.scm4j.releaser.conf;

import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

public class URLContentLoaderTest {
	
	@Test
	public void testNullContent() throws IOException {
		assertNull(new URLContentLoader().getContentFromUrl(null));
		assertNull(new URLContentLoader().getContentFromUrls(null));
	}

}
