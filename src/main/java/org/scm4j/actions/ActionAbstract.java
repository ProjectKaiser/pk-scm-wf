package org.scm4j.actions;

import java.util.LinkedHashMap;

public abstract class ActionAbstract implements IAction {
	
	protected IAction parentAction;
	protected LinkedHashMap<String, IAction> actions;
	protected Object result;
	private IAction parent;
	
	public ActionAbstract(IAction parentAction) {
		this.parentAction = parentAction;
	}

	@Override
	public IAction getParent() {
		return parentAction;
	}

	@Override
	public LinkedHashMap<String, IAction> getChildren() {
		return actions;
	}

	public Object getResult() {
		return result;
	}
	
	@Override
	public void setParent(IAction parent) {
		this.parent = parent;
	}
	
	@Override
	public Object getChildResult(String name) throws EChildNotFound {
			return parent;
	}
	
}
