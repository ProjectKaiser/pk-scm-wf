package org.scm4j.releaser.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.PrintStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.mockito.Mockito;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.WorkflowTestBase;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Option;

public class CLITest extends WorkflowTestBase {
	
	private static final String TEST_EXCEPTION = "test exception";
	private SCMReleaser mockedWF;
	private IAction mockedAction;
	private PrintStream mockedPS;
	
	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		mockedWF = Mockito.spy(new SCMReleaser());
		mockedAction = Mockito.mock(IAction.class);
		mockedPS = Mockito.mock(PrintStream.class);
	}
	
	@Test
	public void testCommandSTATUS() throws Exception {
		SCMReleaser mockedWF = Mockito.spy(new SCMReleaser());
		IAction mockedAction = Mockito.mock(IAction.class);
		PrintStream mockedPS = Mockito.mock(PrintStream.class);
		Mockito.doReturn(mockedAction).when(mockedWF).getProductionReleaseAction(UNTILL);
		CommandLine cmd = new CommandLine(new String[] {"status", UNTILL});
		
		assertEquals(new CLI().exec(mockedWF, cmd, mockedPS), CLI.EXIT_CODE_OK);
		
		Mockito.verify(mockedWF).getProductionReleaseAction(UNTILL);
		Mockito.verify(mockedAction, Mockito.never()).execute(Mockito.any(IProgress.class));
		Mockito.verify(mockedPS, Mockito.atLeast(1)).println(Mockito.anyString());
	}
	
	@Test
	public void testCommandFORK() throws Exception {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		Mockito.doReturn(mockedAction).when(mockedWF).getProductionReleaseAction(UNTILL, ActionKind.FORK);
		CommandLine cmd = new CommandLine(new String[] {"fork", UNTILL});
		
		assertEquals(new CLI().exec(mockedWF, cmd, mockedPS), CLI.EXIT_CODE_OK);
		
		Mockito.verify(mockedWF).getProductionReleaseAction(UNTILL, ActionKind.FORK);
		Mockito.verify(mockedAction).execute(Mockito.any(IProgress.class));
	}
	
	@Test
	public void testCommandBUILD() throws Exception {
		env.generateFeatureCommit(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "feature added");
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		
		Mockito.doReturn(mockedAction).when(mockedWF).getProductionReleaseAction(UNTILL, ActionKind.BUILD);
		CommandLine cmd = new CommandLine(new String[] {"build", UNTILL});
		
		assertEquals(new CLI().exec(mockedWF, cmd, mockedPS), CLI.EXIT_CODE_OK);
		
		Mockito.verify(mockedWF).getProductionReleaseAction(UNTILL, ActionKind.BUILD);
		Mockito.verify(mockedAction).execute(Mockito.any(IProgress.class));
	}
	
	@Test
	public void testCommandTAG() throws Exception {
		Mockito.doReturn(mockedAction).when(mockedWF).getTagReleaseAction(UNTILL);
		CommandLine cmd = new CommandLine(new String[] {"tag", UNTILL});
		
		assertEquals(new CLI().exec(mockedWF, cmd, mockedPS), CLI.EXIT_CODE_OK);
		
		Mockito.verify(mockedWF).getTagReleaseAction(UNTILL);
		Mockito.verify(mockedAction).execute(Mockito.any(IProgress.class));
	}
	
	@Test
	public void testExceptions() throws Exception {
		RuntimeException mockedException = Mockito.spy(new RuntimeException(TEST_EXCEPTION));
		Mockito.doThrow(mockedException).when(mockedWF).getProductionReleaseAction(UNTILL);
		CommandLine cmd = new CommandLine(new String[] {"status", UNTILL});
		
		CLI cli = new CLI();
		try {
			cli.exec(mockedWF, cmd, mockedPS);
			fail();
		} catch (RuntimeException e) {
			assertEquals(mockedException, e);
		}
		Mockito.verify(mockedWF).getProductionReleaseAction(UNTILL);
		Mockito.verify(mockedAction, Mockito.never()).execute(Mockito.any(IProgress.class));
	}
	
	@Test 
	public void testEConfig() throws Exception {
		assertEquals(new CLI().exec(new String[] {"wrong command", UNTILL, Option.DELAYED_TAG.getStrValue()}), CLI.EXIT_CODE_ERROR);
	}
	
	@Test
	public void testMainExitCodeOK() throws Exception {
		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_OK);
		CLI.main(new String[] {"status", UNTILL});
	}
	
	@Test
	public void testMainExitCodeERROR() throws Exception {
		exit.expectSystemExitWithStatus(CLI.EXIT_CODE_ERROR);
		CLI.main(new String[] {"wrong command", UNTILL});
	}
	
}
