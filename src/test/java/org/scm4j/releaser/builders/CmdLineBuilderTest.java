package org.scm4j.releaser.builders;

import org.apache.commons.io.input.ReaderInputStream;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.exceptions.EBuilder;
import org.scm4j.releaser.testutils.TestEnvironment;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class CmdLineBuilderTest {

	private static final String TEST_PROCESS_OUTPUT = "test process output";
	private static final String TEST_CMD_LINE = "cmd.exe /C \"sdsds ddgfgf\" gradlew.bat build";
	private static final List<String> TEST_CMD_LINE_LIST = Arrays.asList("cmd.exe", "/C", "\"sdsds ddgfgf\"",
			"gradlew.bat", "build");
	private static final String TEST_PROCESS_ERROR = "test process error";
	public static final String TEST_ENV_VAR_NAME = "envVar";
	public static final String TEST_ENV_VAR_VALUE = "value";

	@Test
	public void testBuild() throws Exception {
		CmdLineBuilder clb = spy(new CmdLineBuilder(TEST_CMD_LINE));
		Component comp;
		try (TestEnvironment env = new TestEnvironment()) {
			env.generateTestEnvironmentNoVCS();
			comp = new Component(TestEnvironment.PRODUCT_UNTILLDB);
		}

		File workingFolder = new File(TestEnvironment.TEST_REMOTE_REPO_DIR); 
		StringReader procOutputReader = new StringReader(TEST_PROCESS_OUTPUT);
		InputStream processOutputStream = new ReaderInputStream(procOutputReader, StandardCharsets.UTF_8);
		StringReader procErrorReader = new StringReader(TEST_PROCESS_ERROR);
		InputStream processErrorStream = new ReaderInputStream(procErrorReader, StandardCharsets.UTF_8);
		Map<String, String> envVars = new HashMap<>();
		envVars.put(TEST_ENV_VAR_NAME, TEST_ENV_VAR_VALUE);
		
		Process mockedProc = mock(Process.class);
		IProgress mockedProgress = mock(IProgress.class);
		doReturn(mockedProc).when(clb).getProcess(any(ProcessBuilder.class));
		doReturn(processOutputStream).when(mockedProc).getInputStream();
		doReturn(processErrorStream).when(mockedProc).getErrorStream();
		clb.build(comp, workingFolder, mockedProgress, envVars);

		verify(mockedProgress).reportStatus(TEST_PROCESS_OUTPUT);

		ArgumentCaptor<ProcessBuilder> pbCaptor = ArgumentCaptor.forClass(ProcessBuilder.class);
		verify(clb).getProcess(pbCaptor.capture());
		ProcessBuilder pbToTest = pbCaptor.getValue();
		assertEquals(workingFolder, pbToTest.directory());
		assertTrue(pbToTest.redirectErrorStream());
		assertEquals(TEST_ENV_VAR_VALUE, pbToTest.environment().get(TEST_ENV_VAR_NAME));
		assertTrue(TEST_CMD_LINE_LIST.containsAll(pbToTest.command()));
		assertTrue(pbToTest.command().containsAll(TEST_CMD_LINE_LIST));
		
		doReturn(1).when(mockedProc).waitFor();
		try {
			clb.build(comp, workingFolder, mockedProgress, envVars);
			fail();
		} catch (EBuilder e) {
			assertThat(e.getMessage(), Matchers.allOf(
					Matchers.containsString(TEST_PROCESS_ERROR),
					Matchers.containsString(comp.toString())));
			assertEquals(comp, e.getComp());
		}
	}
}
