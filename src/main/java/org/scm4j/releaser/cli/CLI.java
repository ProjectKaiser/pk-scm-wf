package org.scm4j.releaser.cli;

import java.io.PrintStream;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.actions.PrintAction;
import org.scm4j.releaser.exceptions.EConfig;

public class CLI {
	
	private SCMReleaser releaser;
	private CommandLine cmd;
	private PrintStream ps;
	
	public CommandLine getCmd() {
		return cmd;
	}
	
	public void exec() throws Exception {
		IAction action;
		switch(cmd.getCommand()) {
		case BUILD:
			action = releaser.getProductionReleaseAction(cmd.getProductCoords(), ActionKind.BUILD);
			try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
				action.execute(progress);
			}
			break;
		case FORK:
			action = releaser.getProductionReleaseAction(cmd.getProductCoords(), ActionKind.FORK);
			try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
				action.execute(progress);
			}
			break;
		case STATUS:
			action = releaser.getProductionReleaseAction(cmd.getProductCoords());
			PrintAction pa = new PrintAction();
			pa.print(ps, action);
			break;
		case TAG:
			action = releaser.getTagReleaseAction(cmd.getProductCoords());
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
		releaser = new SCMReleaser(cmd.getOptions());
		ps = System.out;
	}
	
	public CLI(SCMReleaser releaser, CommandLine cmd, PrintStream ps) {
		this.releaser = releaser;
		this.cmd = cmd;
		this.ps = ps;
	}

	public static void main(String[] args) throws Exception {
		new CLI(args).exec();
	}

	public SCMReleaser getWorkflow() {
		return releaser;
	}
}