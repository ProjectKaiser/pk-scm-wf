package org.scm4j.actions;

import java.util.List;
import java.util.Map;

import org.scm4j.commons.progress.IProgress;

public interface IAction {

	void setParent(IAction parent);
	
	Object execute(IProgress progress);

	IAction getParent(); // may be null
	
	List<IAction> getChildActions();
	
	String getName();

	Map<String, Object> getExecutionResults();
	
}
