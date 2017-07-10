package org.scm4j.wf;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.conf.VCSRepository;
import org.scm4j.wf.conf.VCSType;

import com.google.common.io.Resources;

public class VCSRepositoriesTest {
	
	private String urlsStr;
	private String credsStr;
	
	@Before
	public void setUp() throws IOException {
		urlsStr = Resources.toString(Resources.getResource(this.getClass(), "urls.yml"), StandardCharsets.UTF_8);
		credsStr = Resources.toString(Resources.getResource(this.getClass(), "creds.yml"), StandardCharsets.UTF_8); 
	}

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
		assertThat(new Object[] { rep.getName(), rep.getUrl(), rep.getType(), rep.getDevBranch(), rep.getReleaseBanchPrefix() },
				is(new Object[] { "artA1", "http://url.com/svn/prjA", VCSType.SVN, "branches/", "release/" }));
		assertThat(new Object[] { rep.getCredentials().getName(), rep.getCredentials().getPassword() },
				is(new Object[] { "user", "password" }));
	}

	@Test
	public void get2() {
		VCSRepositories reps = new VCSRepositories(urlsStr, credsStr);
		VCSRepository rep = reps.get("abyrvalg");
		assertThat(new Object[] { rep.getName(), rep.getUrl(), rep.getType(), rep.getDevBranch(), rep.getReleaseBanchPrefix() },
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
		reps.get(null);
	}

}
