package org.scm4j.wf.scmactions;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.results.ActionResultVersion;
import org.scm4j.wf.branchstatus.DevelopBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.Version;

import java.util.List;

public class SCMActionUseLastReleaseVersion extends ActionAbstract {
	
	private final Version version;

	public SCMActionUseLastReleaseVersion(Component comp, List<IAction> childActions) {
		super(comp, childActions);
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
		return new ActionResultVersion(getName(), getVersion().toPreviousMinorRelease(), false, null);
	}
}
