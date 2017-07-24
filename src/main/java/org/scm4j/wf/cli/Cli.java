package org.scm4j.wf.cli;

import org.scm4j.wf.ISCMWorkflow;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.PrintAction;

public class Cli {

	public static void main(String[] args) throws Exception {

		String depName = args[0];

		ISCMWorkflow wf = new SCMWorkflow();
		IAction action = wf.getProductionReleaseAction(depName);

		PrintAction pa = new PrintAction();
		pa.print(System.out, action);

//		try (IProgress progress = new ProgressConsole(action.getName(), ">>> ", "<<< ")) {
//			action.execute(progress);
//		}
	}
}
