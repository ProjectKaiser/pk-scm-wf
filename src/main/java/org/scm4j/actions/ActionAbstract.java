package org.scm4j.actions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.wf.IVCSFactory;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.model.VCSRepository;

public abstract class ActionAbstract implements IAction {

	protected IAction parentAction;
	protected List<IAction> childActions;
	protected VCSRepository repo;
	protected String currentBranchName;
	private Map<String, Object> results = new LinkedHashMap<>();

	public Version getLastReleaseVersion() {
		Version ver = getDevVersion();
		ver.setSnapshot(false);
		ver.setMinor(Integer.toString(Integer.parseInt(ver.getMinor()) - 1));
		return ver;
	}
	
	public Version getDevVersion() {
		IVCS vcs = IVCSFactory.getIVCS(repo);
		String verFileContent = vcs.getFileContent(currentBranchName, SCMWorkflow.VER_FILE_NAME);
		return new Version(verFileContent.trim());
	}

	public IVCS getVCS() {
		return IVCSFactory.getIVCS(repo);
	}

	public Map<String, Object> getResults() {
		return parentAction != null ? parentAction.getResults() : results;
	}

	public ActionAbstract(VCSRepository repo, List<IAction> childActions, String currentBranchName) {
		this.repo = repo;
		this.childActions = childActions;
		this.currentBranchName = currentBranchName;
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
