package org.scm4j.releaser.conf;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.releaser.exceptions.EComponentConfigNoUrl;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class VCSRepositoryFactoryTest {

	private VCSRepositoryFactory repoFactory;
	
	@Before
	public void setUp() throws Exception {
		File ccFile = new File(Resources.getResource(this.getClass(), "urls-omap.yml").toURI());
		File credsFile = new File(Resources.getResource(this.getClass(), "creds.yml").toURI());
		IConfigUrls configUrls = new IConfigUrls() {
			@Override
			public String getCCUrls() {
				return ccFile.toString();
			}

			@Override
			public String getCredsUrl() {
				return credsFile.toString();
			}
		};
		repoFactory = new VCSRepositoryFactory();
		repoFactory.load(configUrls);
	}

	@Test
	public void getMy() {
		VCSRepository rep = repoFactory.getVCSRepository("myDiskFormatter");
		assertEquals("myDiskFormatter", rep.getName());
		assertEquals("http://localhost/git/myProjDiskFormatter", rep.getUrl());
		assertEquals(VCSType.GIT, rep.getType());
		assertEquals("B", rep.getReleaseBranchPrefix());
		assertEquals("gradlew", rep.getBuilder().getCommand());
		assertEquals("dev", rep.getDevelopBranch());
		assertEquals(null, rep.getCredentials().getName());
		assertEquals(null, rep.getCredentials().getPassword());
	}

	@Test
	public void get1() {
		VCSRepository rep = repoFactory.getVCSRepository("artA1");
		assertThat(new Object[] { rep.getName(), rep.getUrl(), rep.getType(), rep.getDevelopBranch(), rep.getReleaseBranchPrefix() },
				is(new Object[] { "artA1", "http://url.com/svn/prjA", VCSType.SVN, "branches/", "release/" }));
		assertThat(new Object[] { rep.getCredentials().getName(), rep.getCredentials().getPassword() },
				is(new Object[] { "user", "password" }));
	}

	@Test
	public void get2() {
		VCSRepository rep = repoFactory.getVCSRepository("abyrvalg");
		assertThat(new Object[] { rep.getName(), rep.getUrl(), rep.getType(), rep.getDevelopBranch(), rep.getReleaseBranchPrefix() },
				is(new Object[] { "abyrvalg", "https://github.com/qwerty/abyrvalg", VCSType.SVN, "branches/", "release/" }));
		assertThat(new Object[] { rep.getCredentials().getName(), rep.getCredentials().getPassword() },
				is(new Object[] { "guest", "guest" }));
	}

	@Test
	public void testGitVCSTypeDetermination() {
		VCSRepository repo = repoFactory.getVCSRepository("git1");
		assertEquals(VCSType.GIT, repo.getType());
		repo = repoFactory.getVCSRepository("git2");
		assertEquals(VCSRepositoryFactory.DEFAULT_VCS_TYPE, repo.getType());
	}

	@Test
	public void testSVNAlternativeDetermination() {
		VCSRepository repo = repoFactory.getVCSRepository("svn1");
		assertEquals(VCSType.SVN, repo.getType());
	}

	@Test
	public void testNullConfigUrls() throws IOException {
		IConfigUrls configUrls = new IConfigUrls() {
			@Override
			public String getCCUrls() {
				return null;
			}

			@Override
			public String getCredsUrl() {
				return null;
			}
		};
		// expect no exceptions
		repoFactory = new VCSRepositoryFactory();
		repoFactory.load(configUrls);
	}

	@Test
	public void testNoRepoUrlException() throws IOException {
		IConfigUrls configUrls = new IConfigUrls() {
			@Override
			public String getCCUrls() {
				return "";
			}

			@Override
			public String getCredsUrl() {
				return "";
			}
		};
		VCSRepositoryFactory repoFactory = new VCSRepositoryFactory();
		repoFactory.load(configUrls);
		try {
			repoFactory.getUrl("wrong comp");
			fail();
		} catch (EComponentConfigNoUrl e) {
		}
	}
}

