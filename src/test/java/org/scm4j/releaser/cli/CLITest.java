package org.scm4j.releaser.cli;

import static org.fusesource.jansi.Ansi.ansi;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.scm4j.releaser.testutils.VerificationModeSometime.sometime;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.mockito.Matchers;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.regexconfig.EConfig;
import org.scm4j.releaser.ActionTreeBuilder;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.Constants;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.ExtendedStatusBuilder;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DefaultConfigUrls;
import org.scm4j.releaser.conf.IConfigUrls;
import org.scm4j.releaser.conf.VCSRepositoryFactory;
import org.scm4j.releaser.exceptions.EBuildStatus;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineNoCommand;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineNoProduct;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineUnknownCommand;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineUnknownOption;
import org.scm4j.releaser.testutils.TestEnvironment;

public class CLITest {

	private static final String TEST_CONFIG_CONTENT = "# test config content";
	private static final String TEST_EXCEPTION_MESSAGE = "test exception";
	private static final String UNTILL = "eu.untill:unTill";
	private static final String UNTILLDB = "eu.untill:unTillDb";
	private IAction mockedAction;
	private PrintStream mockedPS;
	private CLI mockedCLI;
	private ActionTreeBuilder mockedActionTreeBuilder;
	private ExtendedStatusBuilder mockedStatusTreeBuilder;
	private ExtendedStatus mockedStatus;
	private VCSRepositoryFactory mockedRepoFactory;

	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();


	@Before
	public void setUp() throws Exception {
		mockedAction = mock(IAction.class);
		doReturn(true).when(mockedAction).isExecutable();
		mockedPS = spy(System.out);
		mockedActionTreeBuilder = mock(ActionTreeBuilder.class);
		mockedStatusTreeBuilder = mock(ExtendedStatusBuilder.class);
		mockedStatus = mock(ExtendedStatus.class);
		mockedRepoFactory = mock(VCSRepositoryFactory.class);
		mockedCLI = spy(new CLI(mockedPS, mockedStatusTreeBuilder, mockedActionTreeBuilder, mockedRepoFactory));
		AnsiConsole.systemInstall();
	}

	@Test
	public void testSystemExit() throws Exception {
		doReturn(CLI.EXIT_CODE_ERROR).when(mockedCLI).exec(any(String[].class));
		clearEnvVars();
		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
		CLI.main(new String[] {});
	}

	@Test
	public void testCommandSTATUS() throws Exception {
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), UNTILL };
		doReturn(mockedStatus).when(mockedStatusTreeBuilder).getAndCacheMinorStatus(eq(UNTILL), any(CachedStatuses.class));

		assertEquals(CLI.EXIT_CODE_OK, mockedCLI.exec(args));

		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedActionTreeBuilder, never()).getActionTreeForkOnly(any(ExtendedStatus.class), any(CachedStatuses.class));
		verify(mockedStatusTreeBuilder).getAndCacheMinorStatus(eq(UNTILL), any(CachedStatuses.class));
		verify(mockedCLI).printStatusTree(any(ExtendedStatus.class));
	}

	@Test
	public void testCommandFORK() throws Exception {
		String[] args = new String[] { CLICommand.FORK.getCmdLineStr(), UNTILL };
		doReturn(mockedStatus).when(mockedStatusTreeBuilder).getAndCacheMinorStatus(eq(UNTILL), any(CachedStatuses.class));
		doReturn(mockedAction).when(mockedActionTreeBuilder).getActionTreeForkOnly(any(ExtendedStatus.class), any(CachedStatuses.class));

		assertEquals(CLI.EXIT_CODE_OK, mockedCLI.exec(args));

		verify(mockedActionTreeBuilder).getActionTreeForkOnly(any(ExtendedStatus.class), any(CachedStatuses.class));
		verify(mockedStatusTreeBuilder).getAndCacheMinorStatus(eq(UNTILL), any(CachedStatuses.class));
		verify(mockedAction).execute(any(IProgress.class));
		verify(mockedCLI, never()).printStatusTree(any(ExtendedStatus.class));
	}

	@Test
	public void testCommandBUILDMinor() throws Exception {
		String[] args = new String[] { CLICommand.BUILD.getCmdLineStr(), UNTILL };
		doReturn(mockedStatus).when(mockedStatusTreeBuilder).getAndCacheMinorStatus(eq(UNTILL), any(CachedStatuses.class));
		doReturn(mockedAction).when(mockedActionTreeBuilder).getActionTreeFull(any(ExtendedStatus.class), any(CachedStatuses.class));

		assertEquals(CLI.EXIT_CODE_OK, mockedCLIExec(args));

		verify(mockedActionTreeBuilder).getActionTreeFull(any(ExtendedStatus.class), any(CachedStatuses.class));
		verify(mockedStatusTreeBuilder).getAndCacheMinorStatus(eq(UNTILL), any(CachedStatuses.class));
		verify(mockedAction).execute(any(IProgress.class));
		verify(mockedCLI, never()).printStatusTree(any(ExtendedStatus.class));
	}

	@Test
	public void testCommandBUILDPatch() throws Exception {
		String coords = UNTILL + ":123.9";
		String[] args = new String[] { CLICommand.BUILD.getCmdLineStr(), coords };
		doReturn(mockedStatus).when(mockedStatusTreeBuilder).getAndCachePatchStatus(eq(coords), any(CachedStatuses.class));
		doReturn(mockedAction).when(mockedActionTreeBuilder).getActionTreeFull(any(ExtendedStatus.class), any(CachedStatuses.class));

		assertEquals(CLI.EXIT_CODE_OK, mockedCLIExec(args));

		verify(mockedActionTreeBuilder).getActionTreeFull(any(ExtendedStatus.class), any(CachedStatuses.class));
		verify(mockedStatusTreeBuilder).getAndCachePatchStatus(eq(coords), any(CachedStatuses.class));
		verify(mockedAction).execute(any(IProgress.class));
		verify(mockedCLI, never()).printStatusTree(any(ExtendedStatus.class));
	}

	@Test
	public void testCommandTAG() throws Exception {
		String[] args = new String[] { CLICommand.TAG.getCmdLineStr(), UNTILL };
		doReturn(mockedAction).when(mockedActionTreeBuilder).getTagAction(UNTILL);

		assertEquals(CLI.EXIT_CODE_OK, mockedCLIExec(args));

		verify(mockedActionTreeBuilder).getTagAction(UNTILL);
		verify(mockedStatusTreeBuilder, never()).getAndCachePatchStatus(any(Component.class), any(CachedStatuses.class));
		verify(mockedAction).execute(any(IProgress.class));
		verify(mockedCLI, never()).printStatusTree(any(ExtendedStatus.class));
	}

	@Test
	public void testRuntimeExceptionsWorkflow() throws Exception {
		EReleaserException mockedException = spy(new EReleaserException(new NullPointerException()));
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), UNTILL };
		doThrow(mockedException).when(mockedCLI).getStatusTree(any(CommandLine.class), any(CachedStatuses.class));

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));

		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedException, never()).printStackTrace(mockedPS);
		verifyException(CLI.EXECUTION_FAILED_MESSAGE + mockedCLI.getLastException().getMessage());
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

			Exception lastException = mockedCLI.getLastException();
			verify(mockedPS, sometime()).println(Matchers.contains(lastException.toString()));
			verify(mockedPS, never()).println(CommandLine.getUsage());
		}
	}

	@Test
	public void testExceptionStackTrace() throws Exception {
		RuntimeException mockedException = spy(new RuntimeException(TEST_EXCEPTION_MESSAGE));
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), UNTILL, Option.STACK_TRACE.getCmdLineStr() };
		doThrow(mockedException).when(mockedCLI).getStatusTree(any(CommandLine.class), any(CachedStatuses.class));

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));

		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedException).printStackTrace(mockedPS);
	}

	@Test
	public void testExceptionNoMessage() throws Exception {
		RuntimeException mockedException = spy(new RuntimeException(TEST_EXCEPTION_MESSAGE));
		doThrow(mockedException).when(mockedCLI).getStatusTree(any(CommandLine.class), any(CachedStatuses.class));
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), UNTILL };

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));

		verify(mockedPS, sometime()).println(Matchers.contains(mockedException.getMessage()));
		verify(mockedAction, never()).execute(any(IProgress.class));
	}

	@Test
	public void testExceptionOnComponentsConfigLoad() throws Exception {
		EConfig testException = new EConfig(TEST_EXCEPTION_MESSAGE);
		doThrow(testException).when(mockedRepoFactory).load(any(IConfigUrls.class));
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), UNTILL };

		assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));

		verify(mockedPS, sometime()).println(Matchers.contains(testException.getMessage()));
		verify(mockedAction, never()).execute(any(IProgress.class));
		verify(mockedCLI).printExceptionConfig(false, testException, mockedPS);
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
	public void testSuccessfulExecutionWithWorkingDirInitFailure() throws Exception {
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), TestEnvironment.PRODUCT_UNTILL };
		Utils.waitForDeleteDir(Constants.BASE_WORKING_DIR);
		clearEnvVars();
		Exception testException = new Exception("test exception");
		try (TestEnvironment env = new TestEnvironment()) {
			env.generateTestEnvironment();
			CLI mockedCLI = spy(new CLI());
			doThrow(testException).when(mockedCLI).initWorkingDir();
			assertEquals(CLI.EXIT_CODE_OK, mockedCLI.exec(args));
			verify(mockedCLI).printExceptionInitDir(eq(false), eq(testException), any(PrintStream.class));
		}
	}

	@Test
	public void testInitWorkingDir() throws Exception {
		String[] args = new String[] {};
		Utils.waitForDeleteDir(Constants.BASE_WORKING_DIR);
		clearEnvVars();
		File customConfigTemplateFile = new File(Constants.BASE_WORKING_DIR, CLI.CONFIG_TEMPLATES.get(0));
		FileUtils.writeStringToFile(customConfigTemplateFile, TEST_CONFIG_CONTENT, StandardCharsets.UTF_8);
		new CLI().exec(args);

		List<String> srcFileNames = new ArrayList<>();
		for (File srcFile : FileUtils.listFiles(getResourceFile(CLI.class), FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter())) {
			srcFileNames.add(srcFile.getName());
		}

		List<String> dstFileNames = new ArrayList<>();
		for (File dstFile : FileUtils.listFiles(Constants.BASE_WORKING_DIR, FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter())) {
			dstFileNames.add(dstFile.getName());
		}

		assertTrue(dstFileNames.containsAll(srcFileNames));
		assertEquals(srcFileNames.size(), dstFileNames.size());
		assertEquals(TEST_CONFIG_CONTENT, FileUtils.readFileToString(customConfigTemplateFile, StandardCharsets.UTF_8));
	}
	
	@Test
	public void testBriefCompInfoOnBuildStatusFailure() throws Exception {
		String[] args = new String[] { CLICommand.STATUS.getCmdLineStr(), UNTILLDB };
		RuntimeException testException = new RuntimeException("test exception");
		try (TestEnvironment env = new TestEnvironment()) {
			env.generateTestEnvironment();
			Component compUnTillDb = new Component(UNTILLDB);
			doThrow(testException).when(mockedRepoFactory).getVCSRepository(compUnTillDb);
			mockedCLI = spy(new CLI(mockedPS, new ExtendedStatusBuilder(mockedRepoFactory), mockedActionTreeBuilder, mockedRepoFactory));
			
			assertEquals(CLI.EXIT_CODE_ERROR, mockedCLI.exec(args));
			assertTrue(mockedCLI.getLastException() instanceof EBuildStatus);
			assertEquals(compUnTillDb, ((EBuildStatus) mockedCLI.getLastException()).getComp());
			verify(mockedPS, sometime()).println(Matchers.contains(((EBuildStatus) mockedCLI.getLastException()).getMessage()));
		}
	}

	private File getResourceFile(Class<?> forClass) throws Exception {
		URL url = forClass.getResource(CLI.CONFIG_TEMPLATES_ROSURCE_PATH);
		return new File(url.toURI());
	}

	@SuppressWarnings("deprecation")
	void clearEnvVars() {
		EnvironmentVariables ev = new EnvironmentVariables();
		ev.set(DefaultConfigUrls.REPOS_LOCATION_ENV_VAR, null);
		ev.set(DefaultConfigUrls.CC_URLS_ENV_VAR, null);
		ev.set(DefaultConfigUrls.CREDENTIALS_URL_ENV_VAR, null);
	}

	private void verifyException(String message) {
		verify(mockedPS, sometime()).println(Matchers.contains(
				ansi().a(Ansi.Attribute.INTENSITY_BOLD).fgRed().a(message).reset().toString()));
	}

	private void verifyException() {
		verifyException(mockedCLI.getLastException().getMessage());
	}

	private void verifyCmdLineException() {
		verifyException();
		verify(mockedPS).println(CommandLine.getUsage());
	}

	int mockedCLIExec(String[] args) {
		int res = mockedCLI.exec(args);
		if (mockedCLI.getLastException() != null) {
			throw mockedCLI.getLastException();
		}
		return res;
	}
}
