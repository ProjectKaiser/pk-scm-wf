package org.scm4j.releaser.branch;

import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.releaser.conf.Component;

public class ReleaseBranch {

	private final List<Component> mdeps;
	private final boolean exists;
	private final String name;
	private final Version version;
	private final Component comp;

	public ReleaseBranch(List<Component> mdeps, boolean exists, String name, Version version, Component comp) {
		this.mdeps = mdeps;
		this.exists = exists;
		this.name = name;
		this.version = version;
		this.comp = comp;
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

	public List<Component> getCRBDeps() {
		return ReleaseBranchBuilder.getMDepsRelease(comp, name);
	}
}
