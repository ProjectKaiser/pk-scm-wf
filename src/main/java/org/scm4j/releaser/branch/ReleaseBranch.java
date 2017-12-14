package org.scm4j.releaser.branch;

import static org.scm4j.releaser.Utils.reportDuration;

import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.Component;

public class ReleaseBranch {

	private final List<Component> mdeps;
	private final boolean exists;
	private final String name;
	private final Version version;
	private final Component comp;
	private final Version devVersion;

	public ReleaseBranch(List<Component> mdeps, boolean exists, String name, Version version, Component comp, Version devVersion) {
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

	public List<Component> getCRBDeps(IProgress progress) {
		if (devVersion == null) {
			// calculated for Patch, no reason to use
			throw new IllegalStateException("unexpected getCRBDeps() call for patch Release Branch");
		}
		if (exists && version.getPatch().equals(Utils.ZERO_PATCH)) {
			// calculated already at ReleasebranchFactory.getCRB();
			return mdeps;
		}
		return reportDuration(() -> ReleaseBranchFactory.getMDepsRelease(comp, name), "getMDepsRelease", comp, progress);
	}

	public Version getDevVersion() {
		return devVersion;
	}
}
