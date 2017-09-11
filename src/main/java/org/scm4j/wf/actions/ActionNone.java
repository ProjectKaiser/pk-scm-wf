package org.scm4j.wf.actions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.conf.Component;

public class ActionNone extends ActionAbstract {
	
	private final String reason;
	
	public ActionNone(Component comp, List<IAction> actions, String reason) {
		super(comp, actions, null);
		this.reason = reason;
	}

	@Override
	public void execute(IProgress progress) {
		for (IAction action : childActions) {
			try (IProgress nestedProgress = progress.createNestedProgress(action.toString())) {
				action.execute(nestedProgress);
			} catch (Exception e) {
				progress.error("execution error: " + e.toString() + ": " + e.getMessage());
				throw new RuntimeException(e);
			} 
		}
	}
	
	public String getReason() {
		return reason;
	}
	
	@Override
	public String toString() {
		return "none " + comp.getCoords().toString() + ". Reason: " + reason;
	}

}
