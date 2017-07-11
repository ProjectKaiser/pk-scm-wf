package org.scm4j.wf;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.results.ActionResultVersion;
import org.scm4j.wf.conf.VCSRepository;
import org.scm4j.wf.conf.Version;

public class SCMActionUseLastReleaseVersion extends ActionAbstract {

	private Version ver;

	public SCMActionUseLastReleaseVersion(VCSRepository repo, List<IAction> actions, String masterBranchName, IVCSWorkspace ws) {
		super(repo, actions, masterBranchName, ws);
		ver = getDevVersion();
	}

	@Override
	public String toString() {
		return "using last release version " + getName() + ":" + ver.toPreviousMinorRelease();
	}

	public Version getVer() {
		return ver;
	}

	@Override
	public Object execute(IProgress progress) {
		progress.reportStatus(toString());
		ActionResultVersion res = new ActionResultVersion(getName(), ver.toPreviousMinorRelease(), false, null);
		return res;
	}
}
