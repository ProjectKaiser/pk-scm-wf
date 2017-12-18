package org.scm4j.releaser.actions;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.PrintStream;
import java.util.Arrays;

public class PrintStatusTest {
	
	@Test
	public void testPrintAction() {
		IAction mockedAction = Mockito.mock(IAction.class);
		IAction mockedActionNested = Mockito.mock(IAction.class);
		Mockito.doReturn(Arrays.asList(mockedActionNested)).when(mockedAction).getChildActions();
		PrintStream mockedPS = Mockito.mock(PrintStream.class);
		
		PrintStatus pa = new PrintStatus();
		pa.print(mockedPS, mockedAction);
		
		Mockito.verify(mockedPS, Mockito.times(2)).println(Mockito.anyString());
	}
}
