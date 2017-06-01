package org.scm4j.actions;

import java.util.List;

import org.scm4j.progress.IProgress;
import org.scm4j.wf.model.VCSRepository;

public class ActionError extends ActionAbstract implements IAction {
	
	private String cause;

	public ActionError(VCSRepository repo, List<IAction> childActions, String masterBranchName, String cause) {
		super(repo, childActions, masterBranchName);
		this.cause = cause;
	}

	@Override
	public Object execute(IProgress progress) {
		return null;
	}

	@Override
	public String toString() {
		return cause;
	}
	
}
