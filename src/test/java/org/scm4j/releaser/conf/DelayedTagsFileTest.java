package org.scm4j.releaser.conf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DelayedTagsFileTest {

	private static final String TEST_REVISION = "test revision";
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
		assertNull(dtf.getRevisitonByUrl(TEST_URL));
		
		dtf.writeUrlRevision(TEST_URL, TEST_REVISION);
		assertEquals(TEST_REVISION, dtf.getRevisitonByUrl(TEST_URL));
		
		dtf.removeRevisionByUrl(TEST_URL);
		assertNull(dtf.getRevisitonByUrl(TEST_URL));
	}
	
	
	@Test
	public void testGetContentException() throws IOException {
		DelayedTagsFile dtf = spy(new DelayedTagsFile());
		IOException testException = new IOException("test exception");
		
		dtf.writeUrlRevision(TEST_URL, TEST_REVISION);
		
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
			dtf.writeUrlRevision(TEST_URL, TEST_REVISION);
			fail();
		} catch (RuntimeException e) {
			assertEquals(testException, e.getCause());
		}
	}
	
	@Test
	public void testToString() throws IOException {
		DelayedTagsFile dtf = spy(new DelayedTagsFile());
		assertEquals(DelayedTagsFile.MISSING_TO_STRING_MESSAGE, dtf.toString());
		
		dtf.writeUrlRevision(TEST_URL, TEST_REVISION);
		assertNotNull(dtf.toString());
		assertFalse(dtf.toString().equals(DelayedTagsFile.MISSING_TO_STRING_MESSAGE));
		
	}
	
}
