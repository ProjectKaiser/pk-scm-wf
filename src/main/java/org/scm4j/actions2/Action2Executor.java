package org.scm4j.actions2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.scm4j.progress.IProgress;

public class Action2Executor extends Action2Abstract implements IAction2Executor{

	private Map<IAction2, Object> actionsResults = new LinkedHashMap<IAction2, Object>();
	private List<IAction2> actions = new ArrayList<IAction2>();
	final private IAction2Executor parent;
	
	public Action2Executor(String name, IAction2Executor parent){
		super(name);
		this.parent = parent;
	}
	
	public Action2Executor addAction(IAction2 action){
		actions.add(action);
		return this;
	}

	@Override
	public Map<IAction2, Object> getActionResults() {
		return  Collections.unmodifiableMap(actionsResults);
	}

	@Override
	public Object execute(IAction2Executor executor, IProgress progress) {
		for(IAction2 a: actions){
			IProgress p = progress.createNestedProgress(a.getName());
			Object res = null;
			try{
				res = a.execute(this, progress);
				p.close();
			} catch (Exception e) {
				p.close(e);
				res = new EActionFailed(this, a, e);
			}
			this.actionsResults.put(a, res);
		}
		return null;
	}

	@Override
	public List<IAction2> getActions() {
		return Collections.unmodifiableList(actions);
	}

	@Override
	public IAction2Executor getParentExecutor() {
		return parent;
	}

	@Override
	public Map<IAction2, Object> queryActionResults(Class<IAction2> actionClass, boolean querySubExecutors) {
		return Collections.unmodifiableMap(actionsResults);
	}

}
