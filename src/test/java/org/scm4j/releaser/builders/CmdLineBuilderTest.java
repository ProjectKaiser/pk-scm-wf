package org.scm4j.releaser.builders;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.TestEnvironment;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.exceptions.EBuilder;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class CmdLineBuilderTest {
	
	private static final String TEST_PROCESS_OUTPUT = "test process output";
	private static final String TEST_CMD_LINE = "test cmd line";
	private static final String TEST_PROCESS_ERROR = "test process error";
	
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
		
		Process mockedProc = mock(Process.class);
		IProgress mockedProgress = mock(IProgress.class);
		doReturn(mockedProc).when(clb).getProcess(workingFolder);
		doReturn(processOutputStream).when(mockedProc).getInputStream();
		doReturn(processErrorStream).when(mockedProc).getErrorStream();
		clb.build(comp, workingFolder, mockedProgress);

		verify(mockedProgress).reportStatus(TEST_PROCESS_OUTPUT);
		
		doReturn(1).when(mockedProc).waitFor();
		try {
			clb.build(comp, workingFolder, mockedProgress);
			fail();
		} catch (EBuilder e) {
			assertThat(e.getMessage(), Matchers.allOf(
					Matchers.containsString(TEST_PROCESS_ERROR),
					Matchers.containsString(comp.toString())));
			assertEquals(comp, e.getComp());
		}
	}
	
	@Test
	public void testParsingCmdLine() throws Exception {
		List<String> cmdEth = Arrays.asList("cmd.exe",  "/C",  "\"sdsds ddgfgf\"", "gradlew.bat", "build"); 
		CmdLineBuilder cmd = new CmdLineBuilder(StringUtils.join(cmdEth, " "));
		assertTrue(cmdEth.containsAll(cmd.getProcessBuilder().command()));
		assertTrue(cmd.getProcessBuilder().command().containsAll(cmdEth));
	}
}
