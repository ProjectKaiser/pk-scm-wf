package org.scm4j.wf.cli;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.ActionKind;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.PrintAction;
import org.scm4j.wf.exceptions.EConfig;
import org.scm4j.wf.cli.CommandLine;

public class Cli {

	public static void main(String[] args) throws Exception {
		CommandLine cmd;
		try {
			cmd = new CommandLine(args);
		} catch (EConfig e) {
			System.out.println(e.getMessage());
			System.out.println(CommandLine.getUsage());
			System.exit(1);
			return;
			
		}
		
		try {
			SCMWorkflow wf = new SCMWorkflow(cmd.getOptions());
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
				pa.print(System.out, action);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}