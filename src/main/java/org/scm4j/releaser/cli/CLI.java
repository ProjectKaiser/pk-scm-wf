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
	
	public static int EXIT_CODE_OK = 0;
	public static int EXIT_CODE_ERROR = 1;
	
	public int exec( SCMReleaser releaser, CommandLine cmd, PrintStream ps) throws Exception {
		
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
		return EXIT_CODE_OK;
	}
	
	public int exec(String[] args) throws Exception {
		CommandLine cmd;
		try {
			cmd = new CommandLine(args);
		} catch (EConfig e) {
			System.out.println(e.getMessage());
			System.out.println(CommandLine.getUsage());
			return EXIT_CODE_ERROR;
		}
		return exec(new SCMReleaser(cmd.getOptions()), cmd, System.out);
	}
	
	public static void main(String[] args) throws Exception {
		System.exit(new CLI().exec(args));
	}
}