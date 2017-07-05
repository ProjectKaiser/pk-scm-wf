package org.scm4j.wf;

import java.util.List;

import org.scm4j.actions.ActionAbstract;
import org.scm4j.actions.IAction;
import org.scm4j.actions.results.ActionResultTag;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.model.VCSRepository;

public class SCMActionUseExistingTag extends ActionAbstract {
	
	private VCSTag tag;

	public SCMActionUseExistingTag(VCSRepository repo, List<IAction> childActions, String currentBranchName,
			IVCSWorkspace ws, VCSTag tag) {
		super(repo, childActions, currentBranchName, ws);
		this.tag = tag;
	}
	
	public VCSTag getTag() {
		return tag;
	}
	
	@Override
	public String toString() {
		return "using existing tag for " + getName() + ": " + tag.toString();
	}

	@Override
	public Object execute(IProgress progress) {
		progress.reportStatus(toString());
		ActionResultTag res = new ActionResultTag(getName(), tag);
		return res;
	}

	
}
