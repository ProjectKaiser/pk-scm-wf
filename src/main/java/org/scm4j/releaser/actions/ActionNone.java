package org.scm4j.releaser.actions;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.branch.ReleaseBranch;

import java.util.List;

public class ActionNone extends ActionAbstract {
	
	private final String reason;
	
	public ActionNone(ReleaseBranch crb, List<IAction> childActions, String reason) {
		super(crb.getComponent(), childActions);
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
