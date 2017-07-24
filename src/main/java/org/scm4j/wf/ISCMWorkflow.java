package org.scm4j.wf;

import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.conf.Component;

public interface ISCMWorkflow {

	IAction getProductionReleaseAction(String componentName);
	
	IAction getTagReleaseAction(Component comp);

}
