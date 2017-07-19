package org.scm4j.wf.scmactions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.results.ActionResultVersion;
import org.scm4j.wf.branchstatus.DevelopBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.Version;

public class SCMActionUseLastReleaseVersion extends ActionAbstract {
	
	private Version version;

	public SCMActionUseLastReleaseVersion(Component comp, List<IAction> actions) {
		super(comp, actions);
		DevelopBranch db = new DevelopBranch(comp);
		version = db.getVersion();
	}

	@Override
	public String toString() {
		return "using last release version " + getName() + ":" + getVersion().toPreviousMinorRelease();
	}

	public Version getVersion() {
		return version;
	}

	@Override
	public Object execute(IProgress progress) {
		progress.reportStatus(toString());
		ActionResultVersion res = new ActionResultVersion(getName(), getVersion().toPreviousMinorRelease(), false, null);
		return res;
	}
}
