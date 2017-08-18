package org.scm4j.wf.cli;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.ActionKind;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.PrintAction;
import org.scm4j.wf.conf.Component;

public class Cli {

	public static void main(String[] args) throws Exception {

		String depName = args[0];

		SCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(depName, ActionKind.FORK);

		PrintAction pa = new PrintAction();
		pa.print(System.out, action);

		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
	}
}
