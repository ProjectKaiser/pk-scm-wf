package org.scm4j.actions2;

import java.util.List;
import java.util.Map;

import org.scm4j.progress.IProgress;

public interface IAction2Executor extends IAction2{
	/**
	 * @return sorted Map if all completed actions
	 */
	Map<IAction2, Object> getActionResults();
	
	/**
	 * @param actionClass Defines actions results should be queried from. null means all classes.
	 * @param querySubExecutors  if true sub-executors of current executor will be queried, otherwise only current executor will be used
	 * @return
	 */
	Map<IAction2, Object> queryActionResults(Class<IAction2> actionClass, boolean querySubExecutors);
	
	List<IAction2> getActions();
	/**
	 * @return null for root executor
	 */
	IAction2Executor getParentExecutor();
	
	Object execute(IAction2Executor executor, IProgress progress);
}
