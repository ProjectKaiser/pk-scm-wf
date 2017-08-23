package org.scm4j.wf.cli;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.PrintAction;
import org.scm4j.wf.conf.Option;

public class Cli {

	public static void main(String[] args) throws Exception {

		String depName = args[0];
		
		List<Option> options = SCMWorkflow.parseArgs(args);

		SCMWorkflow wf = new SCMWorkflow(options);
		IAction action = wf.getProductionReleaseAction(depName);

		PrintAction pa = new PrintAction();
		pa.print(System.out, action);

		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
	}
}