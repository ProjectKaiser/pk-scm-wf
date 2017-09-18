package org.scm4j.releaser.builders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.input.ReaderInputStream;
import org.junit.Test;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.TestEnvironment;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.exceptions.EBuilder;

public class CmdLineBuilderTest {
	
	private static final String TEST_PROCESS_OUTPUT = "test process output";
	private static final String TEST_CMD_LINE = "test cmd line";
	private static final String TEST_PROCESS_ERROR = "test process error";
	
	@Test
	public void testCmdLineBuilder() throws Exception {
		CmdLineBuilder clb = spy(new CmdLineBuilder(TEST_CMD_LINE));
		Component comp = new Component(TestEnvironment.PRODUCT_UNTILLDB);
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
			assertEquals(TEST_PROCESS_ERROR, e.getMessage());
		}
	}
}
