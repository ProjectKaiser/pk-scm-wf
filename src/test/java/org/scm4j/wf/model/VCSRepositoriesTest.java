package org.scm4j.wf.model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class VCSRepositoriesTest {

	private String urlsStr = ""
			+ "artA1|artA2:\n"
			+ " url: http://url.com/svn/prjA\n"
			+ "my(.*):\n"
			+ " url: http://localhost/git/myProj$1\n"
			+ " type: git\n"
			+ " releaseBanchPrefix: B\n"
			+ " devBranch: null\n"
			+ ".*:\n"
			+ " url: https://github.com/qwerty/$0\n"
			+ " type: svn\n"
			+ " devBranch: branches/";
	private String credsStr = ""
			+ "https?://url\\.com.*:\n"
			+ " name: user\n"
			+ " password: password\n"
			+ "http://localhost.*:\n"
			+ " name: null\n"
			+ " password: null\n"
			+ ".*:\n"
			+ " name: guest\n"
			+ " password: guest\n";


	@Test
	public void getMy() {
		VCSRepositories reps = new VCSRepositories(urlsStr, credsStr);
		VCSRepository rep = reps.get("myDiskFormatter");
		assertEquals("myDiskFormatter", rep.getName());
		assertEquals("http://localhost/git/myProjDiskFormatter", rep.getUrl());
		assertEquals(VCSType.GIT, rep.getType());
		assertEquals("B", rep.getReleaseBanchPrefix());
		assertNull(rep.getDevBranch());
		assertEquals(null, rep.getCredentials().getName());
		assertEquals(null, rep.getCredentials().getPassword());
	}
	@Test
	public void get1() {
		VCSRepositories reps = new VCSRepositories(urlsStr, credsStr);
		VCSRepository rep = reps.get("artA1");
		assertThat(new Object[] {rep.getName(), rep.getUrl(), rep.getType(), rep.getDevBranch(), rep.getReleaseBanchPrefix()},
				is(new Object[] {"artA1", "http://url.com/svn/prjA", VCSType.SVN, "branches/", "release/"}));
		assertThat(new Object[] {rep.getCredentials().getName(), rep.getCredentials().getPassword()},
				is(new Object[] {"user", "password"}));
	}

	@Test
	public void get2() {
		VCSRepositories reps = new VCSRepositories(urlsStr, credsStr);
		VCSRepository rep = reps.get("abyrvalg");
		assertThat(new Object[] {rep.getName(), rep.getUrl(), rep.getType(), rep.getDevBranch(), rep.getReleaseBanchPrefix()},
				is(new Object[] {"abyrvalg", "https://github.com/qwerty/abyrvalg", VCSType.SVN, "branches/", "release/"}));
		assertThat(new Object[] {rep.getCredentials().getName(), rep.getCredentials().getPassword()},
				is(new Object[] {"guest", "guest"}));
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
		reps.get(null);
	}

}
