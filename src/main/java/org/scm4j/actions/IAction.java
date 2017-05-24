package org.scm4j.actions;

import java.util.LinkedHashMap;

import org.scm4j.progress.IProgress;

public interface IAction {

	void setParent(IAction parent);
	
	void execute(IProgress progress);

	IAction getParent(); // may be null
	
	LinkedHashMap<String, IAction> getChildren(); // not null
	
	Object getResult(); // may be null

	Object getChildResult(String name) throws EChildNotFound;
	
}
