package org.scm4j.releaser.cli;

import static org.junit.Assert.*;

import org.junit.Test;
import org.scm4j.releaser.cli.CLICommand;
import org.scm4j.releaser.cli.CommandLine;
import org.scm4j.releaser.conf.Option;
import org.scm4j.releaser.exceptions.EConfig;

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
		assertTrue(cmd.getOptions().isEmpty());
		
		cmd = new CommandLine(new String[] {CLICommand.STATUS.getStrValue(), TEST_COORDS, "wrong option"});
		assertTrue(cmd.getOptions().isEmpty());
		
		cmd = new CommandLine(new String[] {CLICommand.STATUS.getStrValue(), TEST_COORDS, "wrong option", Option.DELAYED_TAG.getStrValue()});
		assertFalse(cmd.getOptions().isEmpty());
		assertEquals(Option.DELAYED_TAG, cmd.getOptions().get(0));
	}
}
