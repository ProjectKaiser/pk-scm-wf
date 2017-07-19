package org.scm4j.wf.actions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.conf.Component;

public class ActionNone extends ActionAbstract {
	
	private String reason;
	
	public ActionNone(Component comp, List<IAction> actions, String reason) {
		super(comp, actions);
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
