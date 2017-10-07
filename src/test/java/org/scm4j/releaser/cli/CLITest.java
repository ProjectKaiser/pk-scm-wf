package org.scm4j.releaser.cli;

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
import org.scm4j.releaser.exceptions.EConfig;

import java.io.PrintStream;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class CLITest {

	private static final String TEST_EXCEPTION = "test exception";
	private static final String UNTILL = "eu.untill:unTill";
	private SCMReleaser mockedWF;
	private IAction mockedAction;
	private PrintStream mockedPS;

	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();

	@Before
	public void setUp() throws Exception {
		mockedWF = mock(SCMReleaser.class);
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
		CommandLine cmd = new CommandLine(new String[] { "status", UNTILL });

		assertEquals(new CLI().exec(mockedWF, cmd, mockedPS), CLI.EXIT_CODE_OK);

		verify(mockedWF).getActionTree(UNTILL);
		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedPS, atLeast(1)).println(anyString());
	}

	@Test
	public void testCommandFORK() throws Exception {
		doReturn(mockedAction).when(mockedWF).getActionTree(UNTILL, ActionKind.FORK);
		CommandLine cmd = new CommandLine(new String[] { "fork", UNTILL });

		assertEquals(new CLI().exec(mockedWF, cmd, mockedPS), CLI.EXIT_CODE_OK);

		verify(mockedWF).getActionTree(UNTILL, ActionKind.FORK);
		verify(mockedAction).execute(any(IProgress.class));
	}

	@Test
	public void testCommandBUILD() throws Exception {
		doReturn(mockedAction).when(mockedWF).getActionTree(UNTILL, ActionKind.BUILD);
		CommandLine cmd = new CommandLine(new String[] { "build", UNTILL });

		assertEquals(new CLI().exec(mockedWF, cmd, mockedPS), CLI.EXIT_CODE_OK);

		verify(mockedWF).getActionTree(UNTILL, ActionKind.BUILD);
		verify(mockedAction).execute(any(IProgress.class));
	}

	@Test
	public void testCommandTAG() throws Exception {
		doReturn(mockedAction).when(mockedWF).getActionTree(UNTILL);
		CommandLine cmd = new CommandLine(new String[] { "tag", UNTILL });

		assertEquals(new CLI().exec(mockedWF, cmd, mockedPS), CLI.EXIT_CODE_OK);

		verify(mockedWF).getActionTree(UNTILL);
		verify(mockedAction).execute(any(IProgress.class));
	}

	@Test
	public void testExceptions() throws Exception {
		RuntimeException mockedException = spy(new RuntimeException(TEST_EXCEPTION));
		doThrow(mockedException).when(mockedWF).getActionTree(UNTILL);
		CommandLine cmd = new CommandLine(new String[] { "status", UNTILL });

		CLI cli = new CLI();
		try {
			cli.exec(mockedWF, cmd, mockedPS);
			fail();
		} catch (RuntimeException e) {
			assertEquals(mockedException, e);
		}
		verify(mockedWF).getActionTree(UNTILL);
		verify(mockedAction, never()).execute(any(IProgress.class));
	}

	@Test
	public void testEConfig() throws Exception {
		assertEquals(CLI.EXIT_CODE_ERROR,
				new CLI().exec(new String[] { "wrong command", UNTILL, Option.DELAYED_TAG.getStrValue() }));
		assertEquals(CLI.EXIT_CODE_ERROR, new CLI().exec(new String[] { "status", "unknown component" }));
	}

	@Test
	public void testMainExitCodeOK() throws Exception {
		try (TestEnvironment env = new TestEnvironment()) {
			env.generateTestEnvironment();
			exit.expectSystemExitWithStatus(CLI.EXIT_CODE_OK);
			CLI.main(new String[] { "status", TestEnvironment.PRODUCT_UNTILL });
		}
	}

	@Test
	public void testMainExitCodeERROR() throws Exception {
		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
		CLI.main(new String[] { "wrong command", UNTILL });
	}

	@Test
	public void testMainExitCodeERRORStacktrace() throws Exception {
		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
		CLI.main(new String[] { "wrong command", UNTILL, Option.STACK_TRACE.getStrValue() });
	}

	@Test
	public void testSetOptions() throws Exception {
		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR); // no repo url for
																// unTill
		CLI.main(new String[] { "status", UNTILL });
		assertTrue(Options.getOptions().isEmpty());

		CLI.main(new String[] { "status", UNTILL, Option.DELAYED_TAG.getStrValue() });
		assertTrue(Options.hasOption(Option.DELAYED_TAG));

		try {
			CLI.main(new String[] { "status", UNTILL, "wrong option" });
			fail();
		} catch (EConfig e) {

		}
	}
}
