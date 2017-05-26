package org.scm4j.actions;

import java.util.List;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.wf.GsonUtils;
import org.scm4j.wf.IVCSFactory;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.VCSRepository;
import org.scm4j.wf.VerFile;

public abstract class ActionAbstract implements IAction {
	
	protected IAction parentAction;
	protected List<IAction> childActions;
	protected Object result;
	protected VCSRepository repo;
	protected String masterBranchName;
	
	public VerFile getVerFile() {
		IVCS vcs = IVCSFactory.getIVCS(repo);
		String verFileContent = vcs.getFileContent(masterBranchName, SCMWorkflow.VER_FILE_NAME);
		return GsonUtils.fromJson(verFileContent, VerFile.class);
	}
	
	public IVCS getVCS() {
		return IVCSFactory.getIVCS(repo);
	}
	
	public ActionAbstract(VCSRepository repo, List<IAction> childActions, String masterBranchName) {
		this.repo = repo;
		this.childActions = childActions;
		this.masterBranchName = masterBranchName;
	}

	@Override
	public IAction getParent() {
		return parentAction;
	}

	@Override
	public List<IAction> getChildActions() {
		return childActions;
	}

	public Object getResult() {
		return result;
	}
	
	@Override
	public void setParent(IAction parentAction) {
		this.parentAction = parentAction;
	}
	
	@Override
	public Object getChildResult(String name) throws EChildNotFound {
		return parentAction;
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
