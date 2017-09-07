package org.scm4j.wf.builders;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;
import org.mockito.Mockito;

public class CmdLineBuilderTest {
	
	private static final String TEST_CMD_LINE = "test cmd line";

	@Test
	public void testCmdLineBuilder() throws IOException {
		CmdLineBuilder builder = new CmdLineBuilder(TEST_CMD_LINE);
		
		
		
		ProcessBuilder mockedPB = Mockito.mock(ProcessBuilder.class);
		Process mockedProcess = Mockito.mock(Process.class);
		Mockito.doReturn(mockedProcess).when(mockedPB).start();
		
		Mockito.doReturn(new Bufferedin).when(mockedProcess).getInputStream();
		
		assertEquals(TEST_CMD_LINE, builder.getCmdLine());
		
		
		
		
		
	}

}
