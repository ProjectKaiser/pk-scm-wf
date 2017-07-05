package org.scm4j.wf;

import org.scm4j.actions.IAction;

public interface ISCMWorkflow {

	IAction getProductionReleaseAction();
	
	IAction getTagReleaseAction();

}
