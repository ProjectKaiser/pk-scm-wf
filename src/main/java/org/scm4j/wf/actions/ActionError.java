	package org.scm4j.wf.actions;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.conf.Component;

import java.util.List;

public class ActionError extends ActionAbstract implements IAction {
	
	private final String cause;

	public ActionError(Component comp, List<IAction> childActions, String cause	) {
		super(comp, childActions);
		this.cause = cause;
	}

	@Override
	public void execute(IProgress progress) {
	}

	@Override
	public String toString() {
		return getCause();
	}

	public String getCause() {
		return cause;
	}
	
}
