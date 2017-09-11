package org.scm4j.wf.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.WorkflowTestBase;
import org.scm4j.wf.actions.ActionKind;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.conf.Option;

@PrepareForTest({System.class, CLI.class})
@RunWith(PowerMockRunner.class)
public class CLITest extends WorkflowTestBase {
	
	private static final String TEST_EXCEPTION = "test exception";
	private SCMWorkflow mockedWF;
	private IAction mockedAction;
	private PrintStream mockedPS;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		mockedWF = Mockito.spy(new SCMWorkflow());
		mockedAction = Mockito.mock(IAction.class);
		mockedPS = Mockito.mock(PrintStream.class);
		PowerMockito.mockStatic(System.class);
	}
	
	@Test
	public void testCommandSTATUS() throws Exception {
		SCMWorkflow mockedWF = Mockito.spy(new SCMWorkflow());
		IAction mockedAction = Mockito.mock(IAction.class);
		PrintStream mockedPS = Mockito.mock(PrintStream.class);
		Mockito.doReturn(mockedAction).when(mockedWF).getProductionReleaseAction(UNTILL);
		CommandLine cmd = new CommandLine(new String[] {"status", UNTILL});
		
		CLI cli = new CLI(mockedWF, cmd, mockedPS);
		cli.exec();
		Mockito.verify(mockedWF).getProductionReleaseAction(UNTILL);
		Mockito.verify(mockedAction, Mockito.never()).execute(Mockito.any(IProgress.class));
		Mockito.verify(mockedPS, Mockito.atLeast(1)).println(Mockito.anyString());
		PowerMockito.verifyStatic();
		System.exit(CLI.EXIT_CODE_OK);
	}
	
	@Test
	public void testCommandFORK() {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		Mockito.doReturn(mockedAction).when(mockedWF).getProductionReleaseAction(UNTILL, ActionKind.FORK);
		CommandLine cmd = new CommandLine(new String[] {"fork", UNTILL});
		
		CLI cli = new CLI(mockedWF, cmd, mockedPS);
		cli.exec();
		Mockito.verify(mockedWF).getProductionReleaseAction(UNTILL, ActionKind.FORK);
		Mockito.verify(mockedAction).execute(Mockito.any(IProgress.class));
		PowerMockito.verifyStatic();
		System.exit(CLI.EXIT_CODE_OK);
	}
	
	@Test
	public void testCommandBUILD() {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		Mockito.doReturn(mockedAction).when(mockedWF).getProductionReleaseAction(UNTILL, ActionKind.BUILD);
		CommandLine cmd = new CommandLine(new String[] {"build", UNTILL});
		
		CLI cli = new CLI(mockedWF, cmd, mockedPS);
		cli.exec();
		Mockito.verify(mockedWF).getProductionReleaseAction(UNTILL, ActionKind.BUILD);
		Mockito.verify(mockedAction).execute(Mockito.any(IProgress.class));
		PowerMockito.verifyStatic();
		System.exit(CLI.EXIT_CODE_OK);
	}
	
	@Test
	public void testCommandTAG() {
		Mockito.doReturn(mockedAction).when(mockedWF).getTagReleaseAction(UNTILL);
		CommandLine cmd = new CommandLine(new String[] {"tag", UNTILL});
		
		CLI cli = new CLI(mockedWF, cmd, mockedPS);
		cli.exec();
		Mockito.verify(mockedWF).getTagReleaseAction(UNTILL);
		Mockito.verify(mockedAction).execute(Mockito.any(IProgress.class));
		PowerMockito.verifyStatic();
		System.exit(CLI.EXIT_CODE_OK);
	}
	
	@Test
	public void testExceptions() {
		RuntimeException mockedException = Mockito.spy(new RuntimeException(TEST_EXCEPTION));
		Mockito.doThrow(mockedException).when(mockedWF).getProductionReleaseAction(UNTILL);
		CommandLine cmd = new CommandLine(new String[] {"status", UNTILL});
		
		CLI cli = new CLI(mockedWF, cmd, mockedPS);
		cli.exec();
		Mockito.verify(mockedWF).getProductionReleaseAction(UNTILL);
		Mockito.verify(mockedAction, Mockito.never()).execute(Mockito.any(IProgress.class));
		Mockito.verify(mockedException).printStackTrace();
		PowerMockito.verifyStatic();
		System.exit(CLI.EXIT_CODE_ERROR);
	}
	
	@Test
	public void testConstructor() {
		CLI cli = new CLI(new String[] {"status", UNTILL, Option.DELAYED_TAG.getStrValue()});
		assertEquals(CLICommand.STATUS, cli.getCmd().getCommand());
		assertEquals(UNTILL, cli.getCmd().getProductCoords());
		assertTrue(cli.getWorkflow().getOptions().contains(Option.DELAYED_TAG));
	}
	
	@Test 
	public void testConstructorExcepton() {
		try {
			new CLI(new String[] {"wrong command", UNTILL, Option.DELAYED_TAG.getStrValue()});
			fail();
		} catch (IllegalArgumentException e) {
		}
		PowerMockito.verifyStatic();
		System.exit(CLI.EXIT_CODE_ERROR);
	}
	
	@Test
	public void testMain() throws Exception {
		String[] args = new String[] {"status", UNTILL};
		//PowerMockito.mockStatic(CLI.class);
		CLI mockedCli = Mockito.spy(new CLI(args));
		PowerMockito.whenNew(CLI.class).withAnyArguments().thenReturn(mockedCli);
		CLI.main(args);
		PowerMockito.verifyNew(CLI.class).withArguments(args);
		Mockito.verify(mockedCli).exec();
		PowerMockito.verifyStatic();
		System.exit(CLI.EXIT_CODE_OK);
	}
	
}
