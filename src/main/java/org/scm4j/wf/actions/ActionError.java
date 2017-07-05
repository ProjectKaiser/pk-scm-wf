	package org.scm4j.wf.actions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.conf.Dep;

public class ActionError extends ActionAbstract implements IAction {
	
	private String cause;

	public ActionError(Dep dep, List<IAction> childActions, String masterBranchName, String cause, IVCSWorkspace ws) {
		super(dep, childActions, masterBranchName, ws);
		this.cause = cause;
	}

	@Override
	public Object execute(IProgress progress) {
		return null;
	}

	@Override
	public String toString() {
		return getCause();
	}

	public String getCause() {
		return cause;
	}
	
}
