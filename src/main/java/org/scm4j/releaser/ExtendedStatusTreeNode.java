package org.scm4j.releaser;

import java.util.LinkedHashMap;
import java.util.Map;

import org.scm4j.commons.Version;
import org.scm4j.commons.coords.Coords;

public class ExtendedStatusTreeNode {

	private final Version latestVersion;
	private final Coords coords;
	private final BuildStatus status;
	private final LinkedHashMap<Coords, ExtendedStatusTreeNode> subComponents;

	public ExtendedStatusTreeNode(Version latestVersion, BuildStatus status,
			LinkedHashMap<Coords, ExtendedStatusTreeNode> subComponents, Coords coords) {
		this.latestVersion = latestVersion;
		this.status = status;
		this.subComponents = subComponents;
		this.coords = coords;
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

	public Coords getCoords() {
		return coords;
	}
}
