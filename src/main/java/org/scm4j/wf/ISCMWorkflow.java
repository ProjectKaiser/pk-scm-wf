package org.scm4j.wf;

import java.util.List;

import org.scm4j.wf.actions.IAction;

public interface ISCMWorkflow {

	IAction getProductionReleaseAction(List<IAction> childActions);
	
	IAction getTagReleaseAction(List<IAction> childActions);

}
