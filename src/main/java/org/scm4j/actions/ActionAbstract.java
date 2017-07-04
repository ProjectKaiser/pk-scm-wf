package org.scm4j.actions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.VCSFactory;
import org.scm4j.wf.conf.DepCoords;
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.model.VCSRepository;

public abstract class ActionAbstract implements IAction {

	protected IAction parentAction;
	protected List<IAction> childActions;
	protected VCSRepository repo;
	protected String currentBranchName;
	private Map<String, List<Object>> executionResults = new LinkedHashMap<>();
	protected IVCSWorkspace ws;
	
	public Version getDevVersion() {
		IVCS vcs = VCSFactory.getIVCS(repo, ws);
		String verFileContent = vcs.getFileContent(currentBranchName, SCMWorkflow.VER_FILE_NAME);
		return new Version(verFileContent.trim());
	}
	
	public DepCoords getDevCoords() {
		IVCS vcs = VCSFactory.getIVCS(repo, ws);
		String verFileContent = vcs.getFileContent(currentBranchName, SCMWorkflow.VER_FILE_NAME);
		return new DepCoords(verFileContent.trim());
	}

	public IVCS getVCS() {
		return VCSFactory.getIVCS(repo, ws);
	}

	public Map<String, List<Object>> getExecutionResults() {
		return parentAction != null ? parentAction.getExecutionResults() : executionResults;
	}

	public ActionAbstract(VCSRepository repo, List<IAction> childActions, String currentBranchName, IVCSWorkspace ws) {
		this.repo = repo;
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
		return repo.getName();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [" + repo.getName() + "]";
	}
	
	@Override
	public IRelease getReleaseIntf() {
		if (this instanceof IRelease) {
			return (IRelease) this;
		} else {
			return null;
		}
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
