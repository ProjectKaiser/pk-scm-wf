package org.scm4j.releaser.conf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.scm4j.releaser.Utils;

public class DefaultConfigUrlsTest {
	
	
	private static final String URL1 = "url1";
	private static final String URL2 = "url2";
	private static final String URL3 = "url3";
	
	@Rule
	public EnvironmentVariables ev = new EnvironmentVariables();
	
	@SuppressWarnings("deprecation")
	@Before
	public void setUp() throws IOException {
		Utils.BASE_WORKING_DIR.mkdirs();
		FileUtils.cleanDirectory(Utils.BASE_WORKING_DIR);
		ev.set(DefaultConfigUrls.REPOS_LOCATION_ENV_VAR, null);
		ev.set(DefaultConfigUrls.CC_URLS_ENV_VAR, null);
		ev.set(DefaultConfigUrls.CREDENTIALS_URL_ENV_VAR, null);
	}
	
	@After
	public void tearDown() throws Exception {
		Utils.waitForDeleteDir(Utils.BASE_WORKING_DIR);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testGetCCUrlsFromDeprecatedEnvVar() {
		ev.set(DefaultConfigUrls.REPOS_LOCATION_ENV_VAR, URL1);
		ev.set(DefaultConfigUrls.CC_URLS_ENV_VAR, URL2);
		DefaultConfigUrls dcu = new DefaultConfigUrls();
		
		assertEquals(URL1, dcu.getCCUrls());
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGetCCUrlsFromEnvVar() {
		DefaultConfigUrls dcu = new DefaultConfigUrls();
		ev.set(DefaultConfigUrls.REPOS_LOCATION_ENV_VAR, null);
		ev.set(DefaultConfigUrls.CC_URLS_ENV_VAR, URL2);
		
		assertEquals(URL2, dcu.getCCUrls());
		
		ev.set(DefaultConfigUrls.CC_URLS_ENV_VAR, null);
		
		assertNull(dcu.getCCUrls());
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGetCCUrlsFromFiles() throws IOException {
		DefaultConfigUrls dcu = new DefaultConfigUrls();
		ev.set(DefaultConfigUrls.REPOS_LOCATION_ENV_VAR, null);
		ev.set(DefaultConfigUrls.CC_URLS_ENV_VAR, null);
		
		DefaultConfigUrls.PRIORITY_CC_FILE.createNewFile();
		
		assertEquals(DefaultConfigUrls.PRIORITY_CC_FILE.toString(), dcu.getCCUrls());
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println();
		pw.println("        # my cool comment ");
		pw.println();
		pw.println("    " + URL2 + " # comment");
		pw.println();
		pw.println(URL3 + " # comment");
		pw.print("  ");
		
		FileUtils.writeStringToFile(DefaultConfigUrls.CC_URLS_FILE, sw.toString(), StandardCharsets.UTF_8);
		
		assertEquals(DefaultConfigUrls.PRIORITY_CC_FILE.toString() + DefaultConfigUrls.URL_SEPARATOR 
				+ URL2 + DefaultConfigUrls.URL_SEPARATOR + URL3, dcu.getCCUrls());
		
		DefaultConfigUrls.PRIORITY_CC_FILE.delete();
		
		assertEquals(URL2 + DefaultConfigUrls.URL_SEPARATOR + URL3, dcu.getCCUrls());
	}
	
	@Test
	public void testGetCredsUrl() throws IOException {
		DefaultConfigUrls dcu = new DefaultConfigUrls();
		ev.set(DefaultConfigUrls.CREDENTIALS_URL_ENV_VAR, URL1);
		DefaultConfigUrls.CREDENTIALS_FILE.createNewFile();
		
		assertEquals(URL1, dcu.getCredsUrl());
		
		// use creds file if no env var
		ev.set(DefaultConfigUrls.CREDENTIALS_URL_ENV_VAR, null);
		
		assertEquals(DefaultConfigUrls.CREDENTIALS_FILE.toString(), dcu.getCredsUrl());
		
		DefaultConfigUrls.CREDENTIALS_FILE.delete();
		
		assertNull(dcu.getCredsUrl());
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testExceptions() throws IOException {
		IOException e = new IOException("test excetpion");
		DefaultConfigUrls dcu = spy(new DefaultConfigUrls());
		ev.set(DefaultConfigUrls.REPOS_LOCATION_ENV_VAR, null);
		ev.set(DefaultConfigUrls.CC_URLS_ENV_VAR, null);
		doThrow(e).when(dcu).getLinesFromCCFile();
		
		try {
			dcu.getCCUrls();
		} catch (RuntimeException e1) {
			assertEquals(e, e1.getCause());
		}
		
	}
}
