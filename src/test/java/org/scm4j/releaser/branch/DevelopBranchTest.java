package org.scm4j.releaser.branch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;

import org.junit.Test;
import org.scm4j.releaser.ActionTreeBuilder;
import org.scm4j.releaser.TestEnvironment;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.exceptions.EComponentConfig;
import org.scm4j.vcs.api.IVCS;

public class DevelopBranchTest {
	
	@Test
	public void testIsNotModifiedIfNoCommits() throws Exception {
		Component mockedComp = spy(new Component(TestEnvironment.PRODUCT_UNTILL));
		IVCS mockedVCS = mock(IVCS.class);
		doReturn(mockedVCS).when(mockedComp).getVCS();
		doReturn(new ArrayList<>()).when(mockedVCS).log(anyString(), anyInt());
		try (TestEnvironment env = new TestEnvironment()) {
			env.generateTestEnvironmentNoVCS();
			DevelopBranch db = new DevelopBranch(mockedComp);
			assertFalse(db.isModified());
		}
	}
	
	@Test
	public void testGetVersionIfNoVersionFile() throws Exception {
		try (TestEnvironment env = new TestEnvironment()) {
			env.generateTestEnvironment();
			Component comp = new Component(TestEnvironment.PRODUCT_UNTILL);
			env.getUnTillVCS().removeFile(comp.getVcsRepository().getDevelopBranch(), ActionTreeBuilder.VER_FILE_NAME, "version file removed");
			DevelopBranch db = new DevelopBranch(comp);
			try {
				db.getVersion();
				fail();
			} catch (EComponentConfig e) {
				
			}
		}
	}
}
