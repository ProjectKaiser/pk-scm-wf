package org.scm4j.releaser.cli;

import org.junit.Test;

import static org.junit.Assert.*;

public class CommandLineTest {
	
	private static final String TEST_COORDS = "coords";

	@Test
	public void testCommandLineParams() {
		CommandLine cmd = new CommandLine(new String[] {CLICommand.STATUS.getStrValue(), TEST_COORDS});
		assertEquals(CLICommand.STATUS, cmd.getCommand());
		assertEquals(TEST_COORDS, cmd.getProductCoords());
	}

	@Test
	public void testPrintUsage() {
		System.out.println(CommandLine.getUsage())	;
	}
}
