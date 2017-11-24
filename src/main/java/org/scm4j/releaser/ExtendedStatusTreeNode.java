package org.scm4j.releaser;

import java.util.Map;

import org.scm4j.commons.Version;
import org.scm4j.commons.coords.Coords;

public class ExtendedStatusTreeNode {

	private final Version latestVersion;
	private final BuildStatus status;
	private final Map<Coords, ExtendedStatusTreeNode> subComponents;

	public ExtendedStatusTreeNode(Version latestVersion, BuildStatus status,
			Map<Coords, ExtendedStatusTreeNode> subComponents) {
		this.latestVersion = latestVersion;
		this.status = status;
		this.subComponents = subComponents;
	}

	public Version getLatestVersion() {
		return latestVersion;
	}

	public BuildStatus getStatus() {
		return status;
	}

	public Map<Coords, ExtendedStatusTreeNode> getSubComponents() {
		return subComponents;
	}

}
