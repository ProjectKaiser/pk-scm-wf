package org.scm4j.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.wf.IVCSFactory;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.VCSRepository;
import org.scm4j.wf.VerFile;

public abstract class ActionAbstract implements IAction {
	
	protected IAction parentAction;
	protected List<IAction> childActions;
	protected VCSRepository repo;
	protected String masterBranchName;
	private Map<String, Object> results = new HashMap<>();

	
	public VerFile getVerFile() {
		IVCS vcs = IVCSFactory.getIVCS(repo);
		String verFileContent = vcs.getFileContent(masterBranchName, SCMWorkflow.VER_FILE_NAME);
		return VerFile.fromFileContent(verFileContent);
	}
	
	public IVCS getVCS() {
		return IVCSFactory.getIVCS(repo);
	}
	
	public Map<String, Object> getResults() {
		return parentAction != null ? parentAction.getResults() : results;
	}
	
	public ActionAbstract(VCSRepository repo, List<IAction> childActions, String masterBranchName) {
		this.repo = repo;
		this.childActions = childActions;
		this.masterBranchName = masterBranchName;
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
}
