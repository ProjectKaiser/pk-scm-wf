package org.scm4j.releaser.conf;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.releaser.conf.EnvVarsConfigSource;
import org.scm4j.releaser.conf.IConfigSource;
import org.scm4j.releaser.conf.VCSRepositories;
import org.scm4j.releaser.exceptions.EConfig;
import org.scm4j.releaser.TestEnvironment;

public class ConfigTest {
	
	private static final String TEST_ENVIRONMENT_DIR = TestEnvironment.TEST_ENVIRONMENT_DIR;
	private String repos;
	private String creds;

	@Before
	public void setUp() {
		repos = null;
		creds = null;
		VCSRepositories.setConfigSource(new IConfigSource() {
			@Override
			public String getReposLocations() {
				return repos;
			}

			@Override
			public String getCredentialsLocations() {
				return creds;
			}
		});
		VCSRepositories.resetDefault();
	}
	
	@After
	public void tearDown() {
		VCSRepositories.resetDefault();
		VCSRepositories.setConfigSource(new EnvVarsConfigSource());
	}
	
	@Test
	public void testNoEnvVars() {
		creds = "";
		try {
			VCSRepositories.getDefault();
			fail();
		} catch (EConfig e) {
			assertNull(e.getCause());
		}
		
		creds = null;
		repos = "";
		
		try {
			VCSRepositories.getDefault();
			fail();
		} catch (EConfig e) {
			assertNull(e.getCause());
		}
		
	}
	
	@Test
	public void testMalformedReposUrl() {
		repos = "malformed url";
		creds = "";
		try {
			VCSRepositories.getDefault();
			fail();
		} catch (EConfig e) {
			assertThat(e.getCause(), instanceOf(MalformedURLException.class));
		}
	}
	
	@Test 
	public void testWrongReposLocation() {
		repos = "file:///c:/wrong/Location";
		creds = "";
		try {
			VCSRepositories.getDefault();
			fail();
		} catch (EConfig e) {
			assertThat(e.getCause(), instanceOf(FileNotFoundException.class));
		}
	}
	
	@Test
	public void testBadReposFileContent() throws IOException {
		File vcsRepos = new File(TEST_ENVIRONMENT_DIR, "vcs-repos");
		vcsRepos.getParentFile().mkdirs();
		vcsRepos.createNewFile();
		FileUtils.writeStringToFile(vcsRepos, "wrong content", StandardCharsets.UTF_8);
		repos = "file:///" + vcsRepos.getAbsolutePath().replace("\\", "/");
		creds = "file:///" + vcsRepos.getAbsolutePath().replace("\\", "/");
		try { 
			VCSRepositories.getDefault();
			fail();
		} catch (EConfig e) {
			assertNotNull(e.getCause());
		}
	}
}
