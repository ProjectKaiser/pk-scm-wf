package org.scm4j.releaser.branch;

import org.scm4j.commons.Version;
import org.scm4j.releaser.conf.Component;

import java.util.List;

public class ReleaseBranchPatch {

	private final List<Component> mdeps;
	private final boolean exists;
	private final String name;
	private final Version version;

	public ReleaseBranchPatch(List<Component> mdeps, boolean exists, String name, Version version) {
		this.mdeps = mdeps;
		this.exists = exists;
		this.name = name;
		this.version = version;
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
}
