package org.scm4j.wf;

import java.util.List;

import org.scm4j.actions.IAction;

public interface ISCMWorkflow {
	
	IAction calculateProductionReleaseAction(String depName);
	
	void execActions(List<IAction> actions);
}
