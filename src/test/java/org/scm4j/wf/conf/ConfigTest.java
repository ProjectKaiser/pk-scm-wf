package org.scm4j.wf.conf;

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
import org.scm4j.wf.TestEnvironment;
import org.scm4j.wf.exceptions.EConfig;

public class ConfigTest {
	
	private static final String TEST_ENVIRONMENT_DIR = TestEnvironment.TEST_ENVIRONMENT_DIR;
	private String repos = null;
	private String creds = null;

	@Before
	public void setUp() {
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
	}
	
	@After
	public void tearDown() {
		VCSRepositories.resetDefault();
		VCSRepositories.setConfigSource(new EnvVarsConfigSource());
	}
	
	@Test
	public void testNoReposEnvVar() {
		creds = "";
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
