package org.scm4j.releaser.branch;

import org.junit.Test;
import org.scm4j.releaser.Constants;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.exceptions.ENoVersionFile;
import org.scm4j.releaser.testutils.TestEnvironment;
import org.scm4j.vcs.api.IVCS;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class DevelopBranchTest {
	
	@Test
	public void testIsNotModifiedIfNoCommits() throws Exception {
		try (TestEnvironment env = new TestEnvironment()) {
			env.generateTestEnvironmentNoVCS();
			Component mockedComp = new Component(TestEnvironment.PRODUCT_UNTILL);
			IVCS mockedVCS = mock(IVCS.class);
			VCSRepository mockedRepo = mock(VCSRepository.class);
			doReturn(mockedVCS).when(mockedRepo).getVCS();
			doReturn(new ArrayList<>()).when(mockedVCS).log(anyString(), anyInt());
			DevelopBranch db = new DevelopBranch(mockedComp, mockedRepo);
			assertFalse(db.isModified());
		}
	}
	
	@Test
	public void testNoVersionFileExcetpion() throws Exception {
		try (TestEnvironment env = new TestEnvironment()) {
			env.generateTestEnvironment();
			Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
			VCSRepository repo = env.getRepoFactory().getVCSRepository(comp);
			env.getUnTillVCS().removeFile(repo.getDevelopBranch(), Constants.VER_FILE_NAME, "version file removed");
			DevelopBranch db = new DevelopBranch(comp, repo);
			try {
				db.getVersion();
				fail();
			} catch (ENoVersionFile e) {
			}
		}
	}
}
