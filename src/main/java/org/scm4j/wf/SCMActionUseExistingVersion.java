package org.scm4j.wf;

import java.util.List;

import org.scm4j.actions.ActionAbstract;
import org.scm4j.actions.IAction;
import org.scm4j.actions.results.ActionResultVersion;
import org.scm4j.progress.IProgress;

public class SCMActionUseExistingVersion extends ActionAbstract {

	private VerFile verFile;

	public SCMActionUseExistingVersion(VCSRepository repo, List<IAction> actions, String masterBranchName) {
		super(repo, actions, masterBranchName);
		verFile = getVerFile();

	}

	@Override
	public String toString() {
		return "using existing version " + getName() + verFile.getVer();
	}

	@Override
	public Object execute(IProgress progress) {
		progress.reportStatus(toString());
		ActionResultVersion res = new ActionResultVersion();
		res.setName(getName());
		res.setVersion(verFile.getVer());
		return res;
	}

}
