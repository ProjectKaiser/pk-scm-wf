package org.scm4j.wf;

import org.scm4j.wf.actions.IAction;

public interface ISCMWorkflow {

	IAction getProductionReleaseAction(String componentName);
	
	IAction getTagReleaseAction(String depName);

}
