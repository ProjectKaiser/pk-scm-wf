package org.scm4j.wf.actions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;

public interface IAction {

	void execute(IProgress progress);

	List<IAction> getChildActions();
	
	String getName();

}
