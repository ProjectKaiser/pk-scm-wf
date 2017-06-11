package org.scm4j.wf.cli;

import org.scm4j.actions.IAction;
import org.scm4j.actions.PrintAction;
import org.scm4j.progress.IProgress;
import org.scm4j.progress.ProgressConsole;
import org.scm4j.wf.ISCMWorkflow;
import org.scm4j.wf.SCMWorkflow;

public class Cli {

	public static void main(String[] args) throws Exception {

		String depName = args[0];

		ISCMWorkflow wf = new SCMWorkflow(depName);
		IAction action = wf.getProductionReleaseAction();

		PrintAction pa = new PrintAction();
		pa.print(System.out, action);

		try (IProgress progress = new ProgressConsole(action.getName(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
	}
}
