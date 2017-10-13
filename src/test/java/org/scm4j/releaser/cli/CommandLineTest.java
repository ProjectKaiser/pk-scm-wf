package org.scm4j.releaser.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class CommandLineTest {
	
	private static final String TEST_COORDS = "coords";

	@Test
	public void testGetUsage() {
		assertFalse(CommandLine.getUsage().isEmpty());
	}
	
	@Test
	public void testCommandLineParams() {
		CommandLine cmd = new CommandLine(new String[] {CLICommand.STATUS.getStrValue(), TEST_COORDS});
		assertEquals(CLICommand.STATUS, cmd.getCommand());
		assertEquals(TEST_COORDS, cmd.getProductCoords());
	}
}
