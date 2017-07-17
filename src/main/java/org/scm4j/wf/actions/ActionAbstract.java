package org.scm4j.wf.actions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.VCSFactory;
import org.scm4j.wf.conf.Component;

public abstract class ActionAbstract implements IAction {

	protected IAction parentAction;
	protected List<IAction> childActions;
	protected Component comp;
	protected String currentBranchName;
	private Map<String, List<Object>> executionResults = new LinkedHashMap<>();
	protected IVCSWorkspace ws;

	public IVCS getVCS() {
		return VCSFactory.getIVCS(comp.getVcsRepository(), ws);
	}

	public Map<String, List<Object>> getExecutionResults() {
		return parentAction != null ? parentAction.getExecutionResults() : executionResults;
	}

	public ActionAbstract(Component comp, List<IAction> childActions, String currentBranchName, IVCSWorkspace ws) {
		this.comp = comp;
		this.childActions = childActions;
		this.currentBranchName = currentBranchName;
		this.ws = ws;
		
		if (childActions != null) {
			for (IAction action : childActions) {
				action.setParent(this);
			}
		}
	}

	@Override
	public IAction getParent() {
		return parentAction;
	}

	@Override
	public List<IAction> getChildActions() {
		return childActions;
	}

	@Override
	public void setParent(IAction parentAction) {
		this.parentAction = parentAction;
	}

	@Override
	public String getName() {
		return comp.getName();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [" + comp.getName() + "]";
	}

	public Object getResult(String name, Class<?> resultClass) {
		resultClass.getClass();
		List<Object> results = getExecutionResults().get(name);
		if (results == null) {
			return null;
		}
		for (Object result : results) {
			if (resultClass.isInstance(result)) {
				return result;
			}
		}
		return null;
	}

	public void addResult(String name, Object res) {
		List<Object> results = getExecutionResults().get(name);
		if (results == null) {
			results = new ArrayList<>();
			getExecutionResults().put(name, results);
		}
		results.add(res);
	}
}
