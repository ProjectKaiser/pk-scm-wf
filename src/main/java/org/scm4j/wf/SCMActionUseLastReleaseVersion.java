package org.scm4j.wf;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.results.ActionResultVersion;
import org.scm4j.wf.conf.Dep;
import org.scm4j.wf.conf.Version;

public class SCMActionUseLastReleaseVersion extends ActionAbstract {

	public SCMActionUseLastReleaseVersion(Dep dep, List<IAction> actions, String masterBranchName, IVCSWorkspace ws) {
		super(dep, actions, masterBranchName, ws);
	}

	@Override
	public String toString() {
		return "using last release version " + getName() + ":" + getVer().toPreviousMinorRelease();
	}

	public Version getVer() {
		return dep.getActualVersion();
	}

	@Override
	public Object execute(IProgress progress) {
		progress.reportStatus(toString());
		ActionResultVersion res = new ActionResultVersion(getName(), getVer().toPreviousMinorRelease(), false, null);
		return res;
	}
}
