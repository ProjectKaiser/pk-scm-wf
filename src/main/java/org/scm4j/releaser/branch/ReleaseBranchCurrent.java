package org.scm4j.releaser.branch;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.VCSRepositoryFactory;

import java.util.List;

import static org.scm4j.releaser.Utils.reportDuration;

public class ReleaseBranchCurrent {

	private final List<Component> mdeps;
	private final boolean exists;
	private final String name;
	private final Version version;
	private final Component comp;
	private final Version devVersion;

	public ReleaseBranchCurrent(List<Component> mdeps, boolean exists, String name, Version version, Component comp, Version devVersion) {
		this.mdeps = mdeps;
		this.exists = exists;
		this.name = name;
		this.version = version;
		this.comp = comp;
		this.devVersion = devVersion;
	}

	public List<Component> getMDeps() {
		return mdeps;
	}

	public boolean exists() {
		return exists;
	}

	public String getName() {
		return name;
	}

	public Version getVersion() {
		return version;
	}

	public List<Component> getCRBMDeps(IProgress progress, VCSRepositoryFactory repoFactory) {
		return reportDuration(() -> ReleaseBranchFactory.getMDepsRelease(comp, name, repoFactory), "get CRB mdeps", comp, progress);
	}

	public Version getDevVersion() {
		return devVersion;
	}
}
