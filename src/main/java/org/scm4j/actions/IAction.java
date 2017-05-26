package org.scm4j.actions;

import java.util.List;

import org.scm4j.progress.IProgress;

public interface IAction {

	void setParent(IAction parent);
	
	Object execute(IProgress progress);

	IAction getParent(); // may be null
	
	List<IAction> getChildActions();
	
	Object getResult(); // may be null

	Object getChildResult(String name) throws EChildNotFound;
	
	String getName();
	
}
