package org.scm4j.actions;

import org.scm4j.progress.IProgress;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.model.VCSRepository;

import java.util.List;

public class ActionNone extends ActionAbstract {

	public ActionNone(VCSRepository repo, List<IAction> actions, String masterBranchName, IVCSWorkspace ws) {
		super(repo, actions, masterBranchName, ws);
	}

	@Override
	public Object execute(IProgress progress) {
		return null;
		
	}
	
	@Override
	public String toString() {
		return "Nothing should be done [" + getName() + "]";
	}

}
