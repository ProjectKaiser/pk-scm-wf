package org.scm4j.releaser.cli;

import org.junit.Test;
import org.scm4j.releaser.exceptions.EConfig;

import static org.junit.Assert.*;

public class CommandLineTest {
	
	private static final String TEST_COORDS = "coords";

	@Test
	public void testGetUsage() {
		assertFalse(CommandLine.getUsage().isEmpty());
	}
	
	@Test
	public void testCommandLineExceptions() {
		try {
			new CommandLine(null);
			fail();
		} catch (EConfig e) {
		}
		
		try {
			new CommandLine(new String[] {});
			fail();
		} catch (EConfig e) {
		}
		
		try {
			new CommandLine(new String[] {"wrong command"});
			fail();
		} catch (EConfig e) {
		}
		
		try {
			new CommandLine(new String[] {"wrong command", "coords"});
			fail();
		} catch (EConfig e) {
		}
		
		try {
			new CommandLine(new String[] {CLICommand.STATUS.getStrValue()});
			fail();
		} catch (EConfig e) {
		}
	}
	
	@Test
	public void testCommandLineParams() {
		CommandLine cmd = new CommandLine(new String[] {CLICommand.STATUS.getStrValue(), TEST_COORDS});
		assertEquals(CLICommand.STATUS, cmd.getCommand());
		assertEquals(TEST_COORDS, cmd.getProductCoords());
	}
}
