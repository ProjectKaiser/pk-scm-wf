package org.scm4j.wf.cli;

import java.io.PrintStream;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.ActionKind;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.PrintAction;
import org.scm4j.wf.exceptions.EConfig;

public class CLI {
	
	private SCMWorkflow wf;
	private CommandLine cmd;
	private PrintStream ps;
	
	public CommandLine getCmd() {
		return cmd;
	}
	
	public void exec() throws Exception {
		IAction action;
		switch(cmd.getCommand()) {
		case BUILD:
			action = wf.getProductionReleaseAction(cmd.getProductCoords(), ActionKind.BUILD);
			try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
				action.execute(progress);
			}
			break;
		case FORK:
			action = wf.getProductionReleaseAction(cmd.getProductCoords(), ActionKind.FORK);
			try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
				action.execute(progress);
			}
			break;
		case STATUS:
			action = wf.getProductionReleaseAction(cmd.getProductCoords());
			PrintAction pa = new PrintAction();
			pa.print(ps, action);
			break;
		case TAG:
			action = wf.getTagReleaseAction(cmd.getProductCoords());
			try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
				action.execute(progress);
			}
			break;
		}
	}
	
	public CLI(String[] args) {
		try {
			cmd = new CommandLine(args);
		} catch (EConfig e) {
			System.out.println(e.getMessage());
			System.out.println(CommandLine.getUsage());
			throw e;
		}
		wf = new SCMWorkflow(cmd.getOptions());
		ps = System.out;
	}
	
	public CLI(SCMWorkflow wf, CommandLine cmd, PrintStream ps) {
		this.wf = wf;
		this.cmd = cmd;
		this.ps = ps;
	}

	public static void main(String[] args) throws Exception {
		new CLI(args).exec();
	}

	public SCMWorkflow getWorkflow() {
		return wf;
	}
}