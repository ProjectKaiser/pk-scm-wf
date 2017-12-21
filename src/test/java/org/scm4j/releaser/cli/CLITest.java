package org.scm4j.releaser.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
import org.mockito.Matchers;
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
	private ExtendedStatus mockedStatus;

	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();

	@Before
	public void setUp() throws Exception {
		mockedAction = mock(IAction.class);
		mockedPS = mock(PrintStream.class);
		mockedActionTreeBuilder = mock(ActionTreeBuilder.class);
		mockedStatusTreeBuilder = mock(ExtendedStatusBuilder.class);
		mockedStatus = mock(ExtendedStatus.class);
		mockedCLI = spy(new CLI(mockedPS, mockedStatusTreeBuilder, mockedActionTreeBuilder, null));
	}

	@After
	public void tearDown() {
		VCSRepositories.resetDefault();
	}
	
	@Test
	public void testSystemExit() throws Exception {
		CLI.setCLIFactory(new ICLIFactory() {
			@Override
			public CLI getCLI() {
				return mockedCLI;
			}
		});
		try {
			doReturn(CLI.EXIT_CODE_ERROR).when(mockedCLI).exec(any(String[].class));
			exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
			CLI.main(new String[] {});
		} finally {
			CLI.setCLIFactory(new CLIFactory());
		}
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
	}

	@Test
	public void testRuntimeExceptionsWorkflow() throws Exception {
		RuntimeException mockedException = spy(new RuntimeException(TEST_EXCEPTION));
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), UNTILL };
		doThrow(mockedException).when(mockedCLI).getActionTree(any(CommandLine.class));

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));

		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedException, never()).printStackTrace(mockedPS);
	}

	@Test
	public void testCmdLineExceptionUnknownCommand() throws Exception {
		String[] args = new String[] { "wrong command", UNTILL };

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));
		
		assertTrue(mockedCLI.getLastException() instanceof ECmdLineUnknownCommand);
		verifyCmdLineException();
	}

	@Test
	public void testCmdLineExceptionNoCommand() throws Exception {
		String[] args = new String[0];
		
		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));
		
		assertTrue(mockedCLI.getLastException() instanceof ECmdLineNoCommand);
		verifyCmdLineException();
	}

	@Test
	public void testCmdLineExceptionNoProduct() throws Exception {
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr() };
		
		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));
		
		assertTrue(mockedCLI.getLastException() instanceof ECmdLineNoProduct);
		verifyCmdLineException();
	}

	@Test
	public void testCmdLineExceptionUnknownOption() throws Exception {
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), TestEnvironment.PRODUCT_UNTILL,
				"unknown-option" };

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));
		
		assertTrue(mockedCLI.getLastException() instanceof ECmdLineUnknownOption);
		verifyCmdLineException();
	}

	@Test
	public void testExceptionUnknownComponent() throws Exception {
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), "unknown:component" };
		try (TestEnvironment te = new TestEnvironment()) {
			te.generateTestEnvironmentNoVCS();
			assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));
			
			// FIXME: get rid of unneccessary verifications
			verify(mockedPS, atLeastOnce()).println(anyString());
			verify(mockedPS, atLeastOnce()).println();
			Exception lastException = mockedCLI.getLastException();
			verify(mockedPS).println(Matchers.contains(lastException.getMessage() == null ? lastException.toString() : lastException.getMessage()));
			verify(mockedPS, never()).println(CommandLine.getUsage());
		}
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

		verify(mockedPS, atLeastOnce()).println(anyString());
		verify(mockedPS, atLeastOnce()).println();
		verify(mockedPS).println(Matchers.contains(mockedException.getMessage()));
		verify(mockedAction, never()).execute(any(IProgress.class));
	}

	@Test
	public void testSuccessfulExecution() throws Exception {
		try (TestEnvironment env = new TestEnvironment()) {
			env.generateTestEnvironment();
			exit.expectSystemExitWithStatus(CLI.EXIT_CODE_OK);
			CLI.main(new String[] { CLICommand.STATUS.getCmdLineStr(), TestEnvironment.PRODUCT_UNTILL });
		}
	}
	
	@Test
	public void testInitWorkingFolder() throws Exception {
		mockedCLI.setConfigSource(new IConfigSource() {
			
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
		
		mockedCLI.exec(args);
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
	
	private void verifyCmdLineException() {
		verify(mockedPS, atLeastOnce()).println(anyString());
		verify(mockedPS, atLeastOnce()).println();
		verify(mockedPS).println(Matchers.contains(mockedCLI.getLastException().getMessage()));
		verify(mockedPS).println(CommandLine.getUsage());
	}
}
