package org.scm4j.releaser.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.PrintStream;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.TestEnvironment;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Option;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.conf.VCSRepositories;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineNoCommand;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineNoProduct;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineUnknownCommand;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineUnknownOption;

public class CLITest {

	private static final String TEST_EXCEPTION = "test exception";
	private static final String UNTILL = "eu.untill:unTill";
	private SCMReleaser mockedReleaser;
	private IAction mockedAction;
	private PrintStream mockedPS;

	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();

	@Before
	public void setUp() throws Exception {
		mockedReleaser = mock(SCMReleaser.class);
		mockedAction = mock(IAction.class);
		mockedPS = mock(PrintStream.class);
	}
	
	@After
	public void tearDown() {
		Options.setOptions(new ArrayList<Option>());
		VCSRepositories.resetDefault();
	}

	@Test
	public void testCommandSTATUS() throws Exception {
		SCMReleaser mockedWF = spy(new SCMReleaser());
		IAction mockedAction = mock(IAction.class);
		PrintStream mockedPS = mock(PrintStream.class);
		doReturn(mockedAction).when(mockedWF).getActionTree(UNTILL);
		CommandLine cmd = new CommandLine(new String[] { CLICommand.STATUS.getStrValue(), UNTILL });

		assertEquals(CLI.EXIT_CODE_OK, new CLI().exec(mockedWF, cmd, mockedPS));

		verify(mockedWF).getActionTree(UNTILL);
		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedPS, atLeast(1)).println(anyString());
	}

	@Test
	public void testCommandFORK() throws Exception {
		doReturn(mockedAction).when(mockedReleaser).getActionTree(UNTILL, ActionKind.FORK_ONLY);
		CommandLine cmd = new CommandLine(new String[] { CLICommand.FORK.getStrValue(), UNTILL });

		assertEquals( CLI.EXIT_CODE_OK, new CLI().exec(mockedReleaser, cmd, mockedPS));

		verify(mockedReleaser).getActionTree(UNTILL, ActionKind.FORK_ONLY);
		verify(mockedAction).execute(any(IProgress.class));
	}

	@Test
	public void testCommandBUILD() throws Exception {
		doReturn(mockedAction).when(mockedReleaser).getActionTree(UNTILL, ActionKind.FULL);
		CommandLine cmd = new CommandLine(new String[] { CLICommand.BUILD.getStrValue(), UNTILL });

		assertEquals( CLI.EXIT_CODE_OK, new CLI().exec(mockedReleaser, cmd, mockedPS));

		verify(mockedReleaser).getActionTree(UNTILL, ActionKind.FULL);
		verify(mockedAction).execute(any(IProgress.class));
	}

	@Test
	public void testCommandTAG() throws Exception {
		doReturn(mockedAction).when(mockedReleaser).getActionTree(UNTILL);
		CommandLine cmd = new CommandLine(new String[] { CLICommand.TAG.getStrValue(), UNTILL });

		assertEquals(CLI.EXIT_CODE_OK, new CLI().exec(mockedReleaser, cmd, mockedPS));

		verify(mockedReleaser).getActionTree(UNTILL);
		verify(mockedAction).execute(any(IProgress.class));
	}

	@Test
	public void testExceptionsWorkflow() throws Exception {
		RuntimeException mockedException = spy(new RuntimeException(TEST_EXCEPTION));
		doThrow(mockedException).when(mockedReleaser).getActionTree(UNTILL);
		CommandLine cmd = new CommandLine(new String[] { CLICommand.STATUS.getStrValue(), UNTILL });

		CLI cli = new CLI();
		try {
			cli.execInternal(mockedReleaser, cmd, mockedPS);
			fail();
		} catch (RuntimeException e) {
			assertEquals(mockedException, e);
		}
		verify(mockedReleaser).getActionTree(UNTILL);
		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedException, never()).printStackTrace(mockedPS);
	}

	@Test
	public void testExceptionUnknownCommand() throws Exception {
		Exception thrown = null;
		String[] args = new String[] { "wrong command", UNTILL };
		CommandLine cmd = new CommandLine(args);
		try {
			new CLI().execInternal(new SCMReleaser(), cmd, System.out);
			fail();
		} catch (ECmdLineUnknownCommand e) {
			thrown = e;
		}
		
		assertEquals(CLI.EXIT_CODE_ERROR, new CLI().exec(mockedReleaser, cmd, mockedPS));
		verify(mockedPS).println(thrown.getMessage());
		verify(mockedPS).println(CommandLine.getUsage());
		
		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
		CLI.main(args);
	}
	
	@Test
	public void testExceptionNoCommand() throws Exception {
		Exception thrown = null;
		String[] args = new String[0];
		CommandLine cmd = new CommandLine(args);
		try {
			new CLI().execInternal(new SCMReleaser(), cmd, System.out);
			fail();
		} catch (ECmdLineNoCommand e) {
			thrown = e;
		}
		assertEquals(CLI.EXIT_CODE_ERROR,
				new CLI().exec(mockedReleaser, cmd, mockedPS));
		verify(mockedPS).println(thrown.getMessage());
		verify(mockedPS).println(CommandLine.getUsage());
		
		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
		CLI.main(args);
	}
	
	@Test
	public void testExceptionNoProduct() throws Exception {
		Exception thrown = null;
		String[] args = new String[] {CLICommand.STATUS.getStrValue()};
		CommandLine cmd = new CommandLine(args);
		try {
			new CLI().execInternal(new SCMReleaser(), cmd, System.out);
			fail();
		} catch (ECmdLineNoProduct e) {
			thrown = e;
		}
		assertEquals(CLI.EXIT_CODE_ERROR,
				new CLI().exec(mockedReleaser, cmd, mockedPS));
		verify(mockedPS).println(thrown.getMessage());
		verify(mockedPS).println(CommandLine.getUsage());
		
		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
		CLI.main(args);
	}
	
	@Test
	public void testExceptionUnknownOption() throws Exception {
		Exception thrown = null;
		String[] args = new String[] {CLICommand.STATUS.getStrValue(), TestEnvironment.PRODUCT_UNTILL, "unknown-option"};
		CommandLine cmd = new CommandLine(args);
		try {
			new CLI().execInternal(new SCMReleaser(), cmd, System.out);
			fail();
		} catch (ECmdLineUnknownOption e) {
			thrown = e;
		}
		assertEquals(CLI.EXIT_CODE_ERROR,
				new CLI().exec(mockedReleaser, cmd, mockedPS));
		verify(mockedPS).println(thrown.getMessage());
		verify(mockedPS).println(CommandLine.getUsage());
		
		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
		CLI.main(args);
	}
	
	@Test
	public void testExceptionUnknownComponent() throws Exception {
		Exception thrown = null;
		String[] args = new String[] {CLICommand.STATUS.getStrValue(), "unknown component"};
		CommandLine cmd = new CommandLine(args);
		try {
			new CLI().execInternal(new SCMReleaser(), cmd, System.out);
			fail();
		} catch (IllegalArgumentException e) {
			thrown = e;
		}
		mockedReleaser = spy(new SCMReleaser());
		assertEquals(CLI.EXIT_CODE_ERROR,
				new CLI().exec(mockedReleaser, cmd, mockedPS));
		verify(mockedPS).println(thrown.toString());
		verify(mockedPS, never()).println(CommandLine.getUsage());
		
		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
		CLI.main(args);
	}
	
	@Test
	public void testExceptionStackTrace() throws Exception {
		RuntimeException mockedException = spy(new RuntimeException(TEST_EXCEPTION));
		doThrow(mockedException).when(mockedReleaser).getActionTree(UNTILL);
		CommandLine cmd = new CommandLine(new String[] { CLICommand.STATUS.getStrValue(), UNTILL, Option.STACK_TRACE.getStrValue() });

		CLI cli = new CLI();
		assertEquals(CLI.EXIT_CODE_ERROR, cli.exec(mockedReleaser, cmd, mockedPS));
		verify(mockedReleaser).getActionTree(UNTILL);
		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedException).printStackTrace(mockedPS);
	}
	
	@Test
	public void testExceptionNoMessage() throws Exception {
		RuntimeException mockedException = spy(new RuntimeException(TEST_EXCEPTION));
		doReturn(null).when(mockedException).getMessage();
		doThrow(mockedException).when(mockedReleaser).getActionTree(UNTILL);
		
		CommandLine cmd = new CommandLine(new String[] { CLICommand.STATUS.getStrValue(), UNTILL });

		CLI cli = new CLI();
		assertEquals(CLI.EXIT_CODE_ERROR, cli.exec(mockedReleaser, cmd, mockedPS));
		verify(mockedPS).println(mockedException.toString());
		verify(mockedReleaser).getActionTree(UNTILL);
		verify(mockedAction, never()).execute(any(IProgress.class));
	}

	@Test
	public void testMainExitCodeOK() throws Exception {
		try (TestEnvironment env = new TestEnvironment()) {
			env.generateTestEnvironment();
			exit.expectSystemExitWithStatus(CLI.EXIT_CODE_OK);
			CLI.main(new String[] { CLICommand.STATUS.getStrValue(), TestEnvironment.PRODUCT_UNTILL });
		}
	}

	@Test
	public void testSetOptions() throws Exception {
		try (TestEnvironment env = new TestEnvironment()) {
			env.generateTestEnvironment();
			assertEquals(CLI.EXIT_CODE_OK, new CLI().exec(new String[] { CLICommand.STATUS.getStrValue(), UNTILL }));
			assertTrue(Options.getOptions().isEmpty());
			
			assertEquals(CLI.EXIT_CODE_OK, new CLI().exec(new String[] { CLICommand.STATUS.getStrValue(), UNTILL, Option.DELAYED_TAG.getStrValue() }));
			assertTrue(Options.hasOption(Option.DELAYED_TAG));
		}
	}
}
