package org.scm4j.releaser.conf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.commons.Version;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DelayedTagsFileTest {

	private static final String TEST_REVISION = "test revision";
	private static final Version TEST_VERSION = new Version("1.0");
	private static final String TEST_URL = "test url";
	
	@Before
	@After
	public void setUp() {
		new DelayedTagsFile().delete();
	}

	@Test
	public void testGetRemoveContent() throws IOException {
		DelayedTagsFile dtf = new DelayedTagsFile();
		
		assertTrue(dtf.getContent().isEmpty());
		assertNull(dtf.getDelayedTagByUrl(TEST_URL));
		
		dtf.writeUrlDelayedTag(TEST_URL, TEST_VERSION, TEST_REVISION);
		assertEquals(TEST_REVISION, dtf.getDelayedTagByUrl(TEST_URL).getRevision());
		
		dtf.removeTagByUrl(TEST_URL);
		assertNull(dtf.getDelayedTagByUrl(TEST_URL));
	}
	
	
	@Test
	public void testGetContentException() throws IOException {
		DelayedTagsFile dtf = spy(new DelayedTagsFile());
		IOException testException = new IOException("test exception");
		
		dtf.writeUrlDelayedTag(TEST_URL, TEST_VERSION, TEST_REVISION);
		
		doThrow(testException).when(dtf).loadContent();
		
		try {
			dtf.getContent();
			fail();
		} catch (RuntimeException e) {
			assertEquals(testException, e.getCause());
		}
	}
	
	@Test
	public void testWriteUrlException() throws IOException {
		DelayedTagsFile dtf = spy(new DelayedTagsFile());
		IOException testException = new IOException("test exception");
		
		doThrow(testException).when(dtf).saveContent(anyString());
		
		try {
			dtf.writeUrlDelayedTag(TEST_URL, TEST_VERSION, TEST_REVISION);
			fail();
		} catch (RuntimeException e) {
			assertEquals(testException, e.getCause());
		}
	}
	
	@Test
	public void testToString() throws IOException {
		DelayedTagsFile dtf = spy(new DelayedTagsFile());
		assertEquals(DelayedTagsFile.MISSING_TO_STRING_MESSAGE, dtf.toString());
		
		dtf.writeUrlDelayedTag(TEST_URL, TEST_VERSION, TEST_REVISION);
		assertNotNull(dtf.toString());
		assertFalse(dtf.toString().equals(DelayedTagsFile.MISSING_TO_STRING_MESSAGE));
		
	}
}
