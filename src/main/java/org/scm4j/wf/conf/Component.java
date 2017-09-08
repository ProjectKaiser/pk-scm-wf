package org.scm4j.wf.conf;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.Coords;
import org.scm4j.vcs.api.IVCS;

public class Component {
	private final Coords coords;

	public VCSRepository getVcsRepository() {
		return VCSRepositories.getDefault().getByName(coords.getName());
	}

	public Component(String coordsStr) {
		coords = new Coords(coordsStr);
	}
	
	public IVCS getVCS() {
		return getVcsRepository().getVcs();
	}
	
	public Coords getCoords() {
		return coords;
	}

	public String getName() {
		return coords.getName();
	}

	@Override
	public String toString() {
		return coords.toString();
	}
	
	public Version getVersion() {
		return coords.getVersion();
	}

	public Component cloneWithDifferentVersion(String versionStr) {
		return new Component(coords.toString(versionStr));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Component component = (Component) o;

		return coords.equals(component.coords);

	}

	@Override
	public int hashCode() {
		return coords.hashCode();
	}
}