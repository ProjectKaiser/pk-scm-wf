package org.scm4j.actions2;

import org.scm4j.progress.IProgress;

public class Action2Error extends Action2Abstract{

	final private String details;

	public Action2Error(String name) {
		super(name);
		details = null;
	}
	
	public Action2Error(String name, String details) {
		super(name);
		this.details = details;
	}

	@Override
	public Object execute(IAction2Executor executor, IProgress progress) throws EAction2Error {
		throw new EAction2Error(getName(), details);
	}

}
