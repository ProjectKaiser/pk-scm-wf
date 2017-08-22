package org.scm4j.wf.actions;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.conf.Component;

import java.util.List;

public class ActionNone extends ActionAbstract {
	
	private final String reason;
	
	public ActionNone(Component comp, List<IAction> actions, String reason) {
		super(comp, actions);
		this.reason = reason;
	}

	@Override
	public void execute(IProgress progress) {
		progress.createNestedProgress(reason);
	}
	
	public String getReason() {
		return reason;
	}
	
	@Override
	public String toString() {
		return "none " + comp.getCoords().toString() + ". Reason: " + reason;
	}

}
