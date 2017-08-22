package org.scm4j.wf.actions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;

public interface IAction {

	void setParent(IAction parent);
	
	void execute(IProgress progress);

	IAction getParent(); // may be null
	
	List<IAction> getChildActions();
	
	String getName();

}
