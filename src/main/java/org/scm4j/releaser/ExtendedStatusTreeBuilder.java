package org.scm4j.releaser;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;

import java.util.ArrayList;
import java.util.List;

public class ExtendedStatusTreeBuilder {
	
	private final CalculatedResult calculatedResult;
	
	public ExtendedStatusTreeBuilder(CalculatedResult calculatedResult) {
		this.calculatedResult = calculatedResult;
	}

	public ExtendedStatus getExtendedStatusTree(Component comp, ActionKind actionKind, CalculatedResult calculatedResult)
			throws Exception {

		List<IAction> childActions = new ArrayList<>();
		IProgress progress = new ProgressConsole();
		calculateResultNoStatus(comp, calculatedResult, progress);

		for (Component mdep : calculatedResult.getMDeps(comp)) {
			childActions.add(getActionTree(mdep, actionKind, calculatedResult));
		}

		calculatedResult.setBuildStatus(comp, () -> getBuildStatus(comp, calculatedResult), progress);

		progress.close();
		return new ExtendedStatus
	}
	

}
