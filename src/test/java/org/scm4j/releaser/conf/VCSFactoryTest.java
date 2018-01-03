package org.scm4j.releaser.conf;

import org.junit.Test;
import org.mockito.Mockito;
import org.scm4j.vcs.GitVCS;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSRepositoryWorkspace;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.svn.SVNVCS;

import static org.junit.Assert.*;

public class VCSFactoryTest {
	
	private static final String PWD = "pwd";
	private static final String NAME = "name";
	private static final String URL = "http://my.url.com.git";

	@Test
	public void testGitCreate() {
		IVCSWorkspace mockedWS = Mockito.mock(IVCSWorkspace.class);
		IVCSRepositoryWorkspace mockedRW = Mockito.mock(IVCSRepositoryWorkspace.class);
		Mockito.doReturn(mockedRW).when(mockedWS).getVCSRepositoryWorkspace(URL);
		Mockito.doReturn(URL).when(mockedRW).getRepoUrl();
		
		IVCS vcs = VCSFactory.getVCS(VCSType.GIT, new Credentials(NAME, PWD, true), URL, mockedWS);
		assertTrue(vcs instanceof GitVCS);
		assertEquals(URL, vcs.getRepoUrl());
	}
	
	@Test
	public void testSVNCreate() {
		IVCSWorkspace mockedWS = Mockito.mock(IVCSWorkspace.class);
		IVCSRepositoryWorkspace mockedRW = Mockito.mock(IVCSRepositoryWorkspace.class);
		Mockito.doReturn(mockedRW).when(mockedWS).getVCSRepositoryWorkspace(URL);
		Mockito.doReturn(URL).when(mockedRW).getRepoUrl();
		IVCS vcs = VCSFactory.getVCS(VCSType.SVN, new Credentials(NAME, PWD, true), URL, mockedWS);
		assertTrue(vcs instanceof SVNVCS);
		assertEquals(URL, vcs.getRepoUrl());
	}
	
	@Test
	public void cover() {
		new VCSFactory();
	}
}
