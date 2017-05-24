package org.scm4j.wf;

import java.util.List;

import org.scm4j.actions.IAction;
import org.scm4j.vcs.api.IVCS;

public interface ISCMWorkflow {
	
	IAction calculateProductionReleaseAction(IVCS vcs, String branchName);
	
	void execActions(List<IAction> actions);
}
