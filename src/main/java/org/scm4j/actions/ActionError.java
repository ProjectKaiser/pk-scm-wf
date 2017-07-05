	package org.scm4j.actions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.conf.VCSRepository;

public class ActionError extends ActionAbstract implements IAction {
	
	private String cause;

	public ActionError(VCSRepository repo, List<IAction> childActions, String masterBranchName, String cause, IVCSWorkspace ws) {
		super(repo, childActions, masterBranchName, ws);
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
