package org.scm4j.releaser.cli;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.ActionTreeBuilder;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.TestEnvironment;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.VCSRepositories;
import org.scm4j.releaser.exceptions.EComponentConfig;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineNoCommand;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineNoProduct;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineUnknownCommand;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineUnknownOption;

import java.io.PrintStream;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CLITest {

	private static final String TEST_EXCEPTION = "test exception";
	private static final String UNTILL = "eu.untill:unTill";
	private IAction mockedAction;
	private PrintStream mockedPS;
	private CLI mockedCLI;
	private ActionTreeBuilder mockedActionTreeBuilder;

	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();

	@Before
	public void setUp() throws Exception {
		mockedAction = mock(IAction.class);
		mockedPS = mock(PrintStream.class);

		mockedCLI = spy(new CLI());
		mockedActionTreeBuilder = spy(new ActionTreeBuilder());
		mockedCLI.setActionBuilder(mockedActionTreeBuilder);
		mockedCLI.setOut(mockedPS);
	}

	@After
	public void tearDown() {
		VCSRepositories.resetDefault();
	}

	@Test
	public void testCommandSTATUS() throws Exception {
		doReturn(mockedAction).when(mockedCLI).getActionTree(any(CommandLine.class));
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), UNTILL };

		assertEquals(CLI.EXIT_CODE_OK, mockedCLI.exec(args));

		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedCLI).printStatusTree(any(ExtendedStatus.class));

		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_OK);
		CLI.main(args);
	}

	@Test
	public void testCommandFORK() throws Exception {
		String[] args = new String[] { CLICommand.FORK.getCmdLineStr(), UNTILL };

		assertEquals(CLI.EXIT_CODE_OK, mockedCLI.exec(args));

		verify(mockedActionTreeBuilder).getActionTreeForkOnly(any(ExtendedStatus.class), any(CachedStatuses.class));
		verify(mockedAction).execute(any(IProgress.class));

		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_OK);
		CLI.main(args);
	}

	@Test
	public void testCommandBUILD() throws Exception {
		doReturn(mockedAction).when(mockedActionTreeBuilder).getActionTreeFull(any(ExtendedStatus.class), any(CachedStatuses.class));
		String[] args = new String[] { CLICommand.BUILD.getCmdLineStr(), UNTILL };

		assertEquals(CLI.EXIT_CODE_OK, mockedCLI.exec(args));

		verify(mockedActionTreeBuilder).getActionTreeFull(any(ExtendedStatus.class), any(CachedStatuses.class));
		verify(mockedAction).execute(any(IProgress.class));

		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_OK);
		CLI.main(args);
	}

	@Test
	public void testCommandTAG() throws Exception {
		doReturn(mockedAction).when(mockedActionTreeBuilder).getTagAction(UNTILL);
		String[] args = new String[] { CLICommand.TAG.getCmdLineStr(), UNTILL };

		assertEquals(CLI.EXIT_CODE_OK, mockedCLI.exec(args));

		verify(mockedActionTreeBuilder).getTagAction(UNTILL);
		verify(mockedAction).execute(any(IProgress.class));

		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_OK);
		CLI.main(args);
	}

	@Test
	public void testExceptionsWorkflow() throws Exception {
		RuntimeException mockedException = spy(new RuntimeException(TEST_EXCEPTION));
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), UNTILL };
		doThrow(mockedException).when(mockedCLI).getActionTree(any(CommandLine.class));

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));

		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedException, never()).printStackTrace(mockedPS);

		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
		CLI.main(args);
	}

	@Test
	public void testExceptionUnknownCommand() throws Exception {
		Exception thrown = null;
		String[] args = new String[] { "wrong command", UNTILL };
		CommandLine cmd = new CommandLine(args);
		try {
			new CLI().validateCommandLine(cmd);
			fail();
		} catch (ECmdLineUnknownCommand e) {
			thrown = e;
		}

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));
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
			new CLI().validateCommandLine(cmd);
			fail();
		} catch (ECmdLineNoCommand e) {
			thrown = e;
		}

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));
		verify(mockedPS).println(thrown.getMessage());
		verify(mockedPS).println(CommandLine.getUsage());

		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
		CLI.main(args);
	}

	@Test
	public void testExceptionNoProduct() throws Exception {
		Exception thrown = null;
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr() };
		CommandLine cmd = new CommandLine(args);
		try {
			new CLI().validateCommandLine(cmd);
			fail();
		} catch (ECmdLineNoProduct e) {
			thrown = e;
		}

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));
		verify(mockedPS).println(thrown.getMessage());
		verify(mockedPS).println(CommandLine.getUsage());

		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
		CLI.main(args);
	}

	@Test
	public void testExceptionUnknownOption() throws Exception {
		Exception thrown = null;
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), TestEnvironment.PRODUCT_UNTILL,
				"unknown-option" };
		CommandLine cmd = new CommandLine(args);
		try {
			new CLI().validateCommandLine(cmd);
			fail();
		} catch (ECmdLineUnknownOption e) {
			thrown = e;
		}

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));
		verify(mockedPS).println(thrown.getMessage());
		verify(mockedPS).println(CommandLine.getUsage());

		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
		CLI.main(args);
	}

	@Test
	public void testExceptionUnknownComponent() throws Exception {
		Exception thrown = null;
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), "unknown:component" };
		CommandLine cmd = new CommandLine(args);
		mockedCLI.setActionBuilder(new ActionTreeBuilder());
		try (TestEnvironment te = new TestEnvironment()) {
			te.generateTestEnvironmentNoVCS();
			try {
				new CLI().getActionTree(cmd);
				fail();
			} catch (EComponentConfig e) {
				thrown = e;
			}

			assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));
			verify(mockedPS).println(thrown.getMessage());
			verify(mockedPS, never()).println(CommandLine.getUsage());

			exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
			CLI.main(args);
		}
	}

	@Test
	public void testExceptionStackTrace() throws Exception {
		RuntimeException mockedException = spy(new RuntimeException(TEST_EXCEPTION));
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), UNTILL, Option.STACK_TRACE.getCmdLineStr() };
		doThrow(mockedException).when(mockedCLI).getActionTree(any(CommandLine.class));

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));

		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedException).printStackTrace(mockedPS);
	}

	@Test
	public void testExceptionNoMessage() throws Exception {
		RuntimeException mockedException = spy(new RuntimeException(TEST_EXCEPTION));
		doThrow(mockedException).when(mockedCLI).getActionTree(any(CommandLine.class));
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), UNTILL };

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));

		verify(mockedPS).println(mockedException.toString());
		verify(mockedAction, never()).execute(any(IProgress.class));
	}

	@Test
	public void testMainExitCodeOK() throws Exception {
		try (TestEnvironment env = new TestEnvironment()) {
			env.generateTestEnvironment();
			exit.expectSystemExitWithStatus(CLI.EXIT_CODE_OK);
			mockedCLI.setActionBuilder(new ActionTreeBuilder());
			CLI.main(new String[] { CLICommand.STATUS.getCmdLineStr(), TestEnvironment.PRODUCT_UNTILL });
		}
	}

	@Test
	public void testUnsupportedCommand() throws Exception {
		CommandLine mockedCmd = mock(CommandLine.class);
		doReturn(CLICommand.UNKNOWN).when(mockedCmd).getCommand();
		try {
			mockedCLI.getActionTree(mockedCmd);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}
}
