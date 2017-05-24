package org.scm4j.actions;

import org.scm4j.progress.IProgress;

public class ActionNone extends ActionAbstract implements IAction {

	public ActionNone(IAction parentAction) {
		super(parentAction);
	}

	@Override
	public void execute(IProgress progress) {
		
	}
	
	@Override
	public String toString() {
		return "Nothing should be done";
	}

}
