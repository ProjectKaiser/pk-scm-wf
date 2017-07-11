package org.scm4j.wf.actions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.conf.Dep;

public class ActionNone extends ActionAbstract {
	
	private String reason;
	
	public ActionNone(Dep dep, List<IAction> actions, String masterBranchName, IVCSWorkspace ws, String reason) {
		super(dep, actions, masterBranchName, ws);
		this.reason = reason;
	}

	@Override
	public Object execute(IProgress progress) {
		return null;
	}
	
	public String getReason() {
		return reason;
	}
	
	@Override
	public String toString() {
		return "Nothing should be done [" + getName() + "]. Reason: " + reason;
	}

}
