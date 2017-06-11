package org.scm4j.actions;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.VCSFactory;
import org.scm4j.wf.conf.DepCoords;
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.model.VCSRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ActionAbstract implements IAction {

	protected IAction parentAction;
	protected List<IAction> childActions;
	protected VCSRepository repo;
	protected String currentBranchName;
	private Map<String, Object> results = new LinkedHashMap<>();
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

	public Map<String, Object> getResults() {
		return parentAction != null ? parentAction.getResults() : results;
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
}
