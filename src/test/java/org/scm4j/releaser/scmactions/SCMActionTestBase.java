package org.scm4j.releaser.scmactions;

import org.junit.Before;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.TestEnvironment;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.conf.VCSType;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;

public class SCMActionTestBase {

	protected Component comp;
	protected IProgress progress;

	@Before
	public void setUp() {
		comp = new Component(TestEnvironment.PRODUCT_UNTILL);
		IVCS vcs = mock(IVCS.class);
		doReturn("11.12.13").when(vcs).getFileContent(anyString(), anyString(), (String) isNull());
		VCSRepository repo = new VCSRepository("test repo", "test url", null, VCSType.GIT, "dev branch", "release branch prefix", vcs, null);
		comp.setRepo(repo);
		progress = mock(IProgress.class);
	}

	protected void testExceptionThrowing(IAction action, Exception testException) {
		try {
			action.execute(progress);
			fail();
		} catch (EReleaserException e) {
			assertEquals(testException, e.getCause() == null ? e : e.getCause());
			verify(progress, atLeast(1)).error(anyString());
		}
	}

	protected void testExceptionThrowing(IAction action, Class<?> exceptionClass) {
		try {
			action.execute(progress);
			fail();
		} catch (EReleaserException e) {
			assertThat(e.getCause(), instanceOf(exceptionClass));
			verify(progress, atLeast(1)).error(anyString());
		}
	}
}
