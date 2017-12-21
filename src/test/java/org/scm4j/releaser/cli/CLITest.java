package org.scm4j.releaser.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.ActionTreeBuilder;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.ExtendedStatusBuilder;
import org.scm4j.releaser.TestEnvironment;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.IConfigSource;
import org.scm4j.releaser.conf.VCSRepositories;
import org.scm4j.releaser.exceptions.EComponentConfig;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineNoCommand;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineNoProduct;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineUnknownCommand;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineUnknownOption;

public class CLITest {

	private static final String TEST_EXCEPTION = "test exception";
	private static final String UNTILL = "eu.untill:unTill";
	private IAction mockedAction;
	private PrintStream mockedPS;
	private CLI mockedCLI;
	private ActionTreeBuilder mockedActionTreeBuilder;
	private ExtendedStatusBuilder mockedStatusTreeBuilder;

	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();
	private ExtendedStatus mockedStatus;

	@Before
	public void setUp() throws Exception {
		mockedAction = mock(IAction.class);
		mockedPS = mock(PrintStream.class);

		mockedCLI = spy(new CLI());
		mockedActionTreeBuilder = mock(ActionTreeBuilder.class);
		mockedStatusTreeBuilder = mock(ExtendedStatusBuilder.class);
		mockedStatus = mock(ExtendedStatus.class);
		CLI.setActionBuilder(mockedActionTreeBuilder);
		CLI.setStatusTreeBuilder(mockedStatusTreeBuilder);
		CLI.setOut(mockedPS);
	}

	@After
	public void tearDown() {
		VCSRepositories.resetDefault();
		CLI.setOut(System.out);
		resetCLIBuilders();
	}

	@Test
	public void testCommandSTATUS() throws Exception {
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), UNTILL };
		doReturn(mockedStatus).when(mockedStatusTreeBuilder).getAndCacheMinorStatus(eq(new Component(UNTILL)), any(CachedStatuses.class));

		assertEquals(CLI.EXIT_CODE_OK, mockedCLI.exec(args));

		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedActionTreeBuilder, never()).getActionTreeForkOnly(any(ExtendedStatus.class), any(CachedStatuses.class));
		verify(mockedStatusTreeBuilder).getAndCacheMinorStatus(eq(new Component(UNTILL)), any(CachedStatuses.class));
		verify(mockedCLI).printStatusTree(any(ExtendedStatus.class));

		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_OK);
		CLI.main(args);
	}

	@Test
	public void testCommandFORK() throws Exception {
		String[] args = new String[] { CLICommand.FORK.getCmdLineStr(), UNTILL };
		doReturn(mockedStatus).when(mockedStatusTreeBuilder).getAndCacheMinorStatus(eq(new Component(UNTILL)), any(CachedStatuses.class));
		doReturn(mockedAction).when(mockedActionTreeBuilder).getActionTreeForkOnly(any(ExtendedStatus.class), any(CachedStatuses.class));

		assertEquals(CLI.EXIT_CODE_OK, mockedCLI.exec(args));

		verify(mockedActionTreeBuilder).getActionTreeForkOnly(any(ExtendedStatus.class), any(CachedStatuses.class));
		verify(mockedStatusTreeBuilder).getAndCacheMinorStatus(eq(new Component(UNTILL)), any(CachedStatuses.class));
		verify(mockedAction).execute(any(IProgress.class));
		verify(mockedCLI, never()).printStatusTree(any(ExtendedStatus.class));

		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_OK);
		CLI.main(args);
	}

	@Test
	public void testCommandBUILDMinor() throws Exception {
		String[] args = new String[] { CLICommand.BUILD.getCmdLineStr(), UNTILL };
		doReturn(mockedStatus).when(mockedStatusTreeBuilder).getAndCacheMinorStatus(eq(new Component(UNTILL)), any(CachedStatuses.class));
		doReturn(mockedAction).when(mockedActionTreeBuilder).getActionTreeFull(any(ExtendedStatus.class), any(CachedStatuses.class));

		assertEquals(CLI.EXIT_CODE_OK, mockedCLI.exec(args));

		verify(mockedActionTreeBuilder).getActionTreeFull(any(ExtendedStatus.class), any(CachedStatuses.class));
		verify(mockedStatusTreeBuilder).getAndCacheMinorStatus(eq(new Component(UNTILL)), any(CachedStatuses.class));
		verify(mockedAction).execute(any(IProgress.class));
		verify(mockedCLI, never()).printStatusTree(any(ExtendedStatus.class));
		
		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_OK);
		CLI.main(args);
	}
	
	@Test
	public void testCommandBUILDPatch() throws Exception {
		String coords = UNTILL + ":123.9";
		String[] args = new String[] { CLICommand.BUILD.getCmdLineStr(), coords };
		doReturn(mockedStatus).when(mockedStatusTreeBuilder).getAndCachePatchStatus(eq(new Component(coords)), any(CachedStatuses.class));
		doReturn(mockedAction).when(mockedActionTreeBuilder).getActionTreeFull(any(ExtendedStatus.class), any(CachedStatuses.class));

		assertEquals(CLI.EXIT_CODE_OK, mockedCLI.exec(args));

		verify(mockedActionTreeBuilder).getActionTreeFull(any(ExtendedStatus.class), any(CachedStatuses.class));
		verify(mockedStatusTreeBuilder).getAndCachePatchStatus(eq(new Component(coords)), any(CachedStatuses.class));
		verify(mockedAction).execute(any(IProgress.class));
		verify(mockedCLI, never()).printStatusTree(any(ExtendedStatus.class));
		
		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_OK);
		CLI.main(args);
	}

	@Test
	public void testCommandTAG() throws Exception {
		String[] args = new String[] { CLICommand.TAG.getCmdLineStr(), UNTILL };
		doReturn(mockedAction).when(mockedActionTreeBuilder).getTagAction(UNTILL);

		assertEquals(CLI.EXIT_CODE_OK, mockedCLI.exec(args));

		verify(mockedActionTreeBuilder).getTagAction(UNTILL);
		verify(mockedStatusTreeBuilder, never()).getAndCachePatchStatus(any(Component.class), any(CachedStatuses.class));
		verify(mockedAction).execute(any(IProgress.class));
		verify(mockedCLI, never()).printStatusTree(any(ExtendedStatus.class));
		
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
		resetCLIBuilders();
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

	private void resetCLIBuilders() {
		CLI.setActionBuilder(new ActionTreeBuilder());
		CLI.setStatusTreeBuilder(new ExtendedStatusBuilder());
	}

	@Test
	public void testExceptionStackTrace() throws Exception {
		RuntimeException mockedException = spy(new RuntimeException(TEST_EXCEPTION));
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), UNTILL, Option.STACK_TRACE.getCmdLineStr() };
		doThrow(mockedException).when(mockedCLI).getStatusTree(any(CommandLine.class), any(CachedStatuses.class));

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));

		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedException).printStackTrace(mockedPS);
	}

	@Test
	public void testExceptionNoMessage() throws Exception {
		RuntimeException mockedException = spy(new RuntimeException(TEST_EXCEPTION));
		doThrow(mockedException).when(mockedCLI).getStatusTree(any(CommandLine.class), any(CachedStatuses.class));
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
			resetCLIBuilders();
			CLI.main(new String[] { CLICommand.STATUS.getCmdLineStr(), TestEnvironment.PRODUCT_UNTILL });
		}
	}
	
	@Test
	public void testInitWorkingFolder() throws Exception {
		CLI.setConfigSource(new IConfigSource() {
			
			@Override
			public String getCredentialsLocations() {
				return null;
			}
			
			@Override
			public String getCompConfigLocations() {
				return null;
			}
		});
		
		String[] args = new String[] {};
		Utils.waitForDeleteDir(Utils.BASE_WORKING_DIR);
		
		new CLI().exec(args);
		Utils.BASE_WORKING_DIR.mkdirs();
		List<String> srcFileNames = new ArrayList<>();
		for (File srcFile : FileUtils.listFiles(Utils.getResourceFile(CLI.class, CLI.CONFIG_TEMPLATES), FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter())) {
			srcFileNames.add(srcFile.getName());
		}
		
		List<String> dstFileNames = new ArrayList<>();
		for (File dstFile : FileUtils.listFiles(Utils.BASE_WORKING_DIR, FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter())) {
			dstFileNames.add(dstFile.getName());
		}
		
		assertTrue(dstFileNames.containsAll(srcFileNames));
		assertEquals(srcFileNames.size(), dstFileNames.size());
	}
}
