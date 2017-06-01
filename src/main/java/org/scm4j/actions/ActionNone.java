package org.scm4j.actions;

import java.util.List;

import org.scm4j.progress.IProgress;
import org.scm4j.wf.model.VCSRepository;

public class ActionNone extends ActionAbstract {

	public ActionNone(VCSRepository repo, List<IAction> actions, String masterBranchName) {
		super(repo, actions, masterBranchName);
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
