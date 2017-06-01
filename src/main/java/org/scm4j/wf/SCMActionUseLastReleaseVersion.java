package org.scm4j.wf;

import java.util.List;

import org.scm4j.actions.ActionAbstract;
import org.scm4j.actions.IAction;
import org.scm4j.actions.results.ActionResultVersion;
import org.scm4j.progress.IProgress;
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.model.VCSRepository;

public class SCMActionUseLastReleaseVersion extends ActionAbstract {

	private Version ver;

	public SCMActionUseLastReleaseVersion(VCSRepository repo, List<IAction> actions, String masterBranchName) {
		super(repo, actions, masterBranchName);
		ver = getLastReleaseVersion();
	}

	@Override
	public String toString() {
		return "using last release version " + getName() + ":" + ver;
	}

	public Version getVer() {
		return ver;
	}

	@Override
	public Object execute(IProgress progress) {
		progress.reportStatus(toString());
		ActionResultVersion res = new ActionResultVersion(getName(), ver.toString(), false);
		return res;
	}
}
