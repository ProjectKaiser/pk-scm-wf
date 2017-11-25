package org.scm4j.releaser;

import org.scm4j.commons.Version;
import org.scm4j.commons.coords.Coords;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExtendedStatus {

	private final Version latestVersion;
	private final BuildStatus status;
	private final Map<Coords, ExtendedStatus> subComponents = new LinkedHashMap<>();

	public ExtendedStatus(Version latestVersion, BuildStatus status,
			LinkedHashMap<Coords, ExtendedStatus> subComponents) {
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

	public Map<Coords, ExtendedStatus> getSubComponents() {
		return subComponents;
	}

}
