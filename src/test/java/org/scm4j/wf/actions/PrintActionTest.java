package org.scm4j.wf.actions;

import java.io.PrintStream;
import java.util.Arrays;

import org.junit.Test;
import org.mockito.Mockito;

public class PrintActionTest {
	
	@Test
	public void testPrintAction() {
		IAction mockedAction = Mockito.mock(IAction.class);
		IAction mockedActionNested = Mockito.mock(IAction.class);
		Mockito.doReturn(Arrays.<IAction>asList(mockedActionNested)).when(mockedAction).getChildActions();
		PrintStream mockedPS = Mockito.mock(PrintStream.class);
		
		PrintAction pa = new PrintAction();
		pa.print(mockedPS, mockedAction);
		
		Mockito.verify(mockedPS, Mockito.times(2)).println(Mockito.anyString());
		
	}
	
}
