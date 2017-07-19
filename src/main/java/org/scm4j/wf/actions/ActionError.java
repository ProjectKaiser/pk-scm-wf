	package org.scm4j.wf.actions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.conf.Component;

public class ActionError extends ActionAbstract implements IAction {
	
	private String cause;

	public ActionError(Component comp, List<IAction> childActions, String cause	) {
		super(comp, childActions);
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
