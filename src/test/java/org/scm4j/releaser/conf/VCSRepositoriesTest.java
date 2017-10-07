package org.scm4j.releaser.conf;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class VCSRepositoriesTest {
	
	private String urlsStr;
	private String credsStr;
	
	@Before
	public void setUp() throws IOException {
		urlsStr = Resources.toString(Resources.getResource(this.getClass(), "urls-omap.yml"), StandardCharsets.UTF_8);
		credsStr = Resources.toString(Resources.getResource(this.getClass(), "creds.yml"), StandardCharsets.UTF_8); 
	}

	@Test
	public void getMy() {
		VCSRepositories reps = new VCSRepositories(urlsStr, credsStr);
		VCSRepository rep = reps.getByName("myDiskFormatter");
		assertEquals("myDiskFormatter", rep.getName());
		assertEquals("http://localhost/git/myProjDiskFormatter", rep.getUrl());
		assertEquals(VCSType.GIT, rep.getType());
		assertEquals("B", rep.getReleaseBranchPrefix());
		assertNull(rep.getDevBranch());
		assertEquals(null, rep.getCredentials().getName());
		assertEquals(null, rep.getCredentials().getPassword());
	}

	@Test
	public void get1() {
		VCSRepositories reps = new VCSRepositories(urlsStr, credsStr);
		VCSRepository rep = reps.getByName("artA1");
		assertThat(new Object[] { rep.getName(), rep.getUrl(), rep.getType(), rep.getDevBranch(), rep.getReleaseBranchPrefix() },
				is(new Object[] { "artA1", "http://url.com/svn/prjA", VCSType.SVN, "branches/", "release/" }));
		assertThat(new Object[] { rep.getCredentials().getName(), rep.getCredentials().getPassword() },
				is(new Object[] { "user", "password" }));
	}

	@Test
	public void get2() {
		VCSRepositories reps = new VCSRepositories(urlsStr, credsStr);
		VCSRepository rep = reps.getByName("abyrvalg");
		assertThat(new Object[] { rep.getName(), rep.getUrl(), rep.getType(), rep.getDevBranch(), rep.getReleaseBranchPrefix() },
				is(new Object[] { "abyrvalg", "https://github.com/qwerty/abyrvalg", VCSType.SVN, "branches/", "release/" }));
		assertThat(new Object[] { rep.getCredentials().getName(), rep.getCredentials().getPassword() },
				is(new Object[] { "guest", "guest" }));
	}

	@Test(expected = NullPointerException.class)
	public void getNull1() {
		new VCSRepositories(null, null);
	}

	@Test(expected = NullPointerException.class)
	public void getNull2() {
		new VCSRepositories(urlsStr, null);
	}

	@Test(expected = NullPointerException.class)
	public void getNull3() {
		VCSRepositories reps = new VCSRepositories(urlsStr, credsStr);
		reps.getByCoords(null);
	}

	@Test
	public void testGitVCSTypeDetermination() {
		VCSRepositories reps = new VCSRepositories(urlsStr, credsStr);
		VCSRepository repo = reps.getByName("git1");
		assertEquals(VCSType.GIT, repo.getType());
		repo = reps.getByName("git2");
		assertEquals(VCSType.GIT, repo.getType());
	}

}
