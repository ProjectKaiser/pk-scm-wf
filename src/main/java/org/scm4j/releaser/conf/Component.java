package org.scm4j.releaser.conf;

import org.scm4j.commons.Coords;
import org.scm4j.commons.Version;
import org.scm4j.vcs.api.IVCS;

public class Component {
	private final Coords coords;
	private VCSRepository repo = null;
	
	public VCSRepository getVcsRepository() {
		return repo == null ? VCSRepositories.getDefault().getByName(coords.getName()) : repo;
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

	public Component clone(String newVersion) {
		return new Component(coords.toString(newVersion));
	}
	
	public Component clone(Version newVersion) {
		return clone(newVersion.toString());
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

	public void setRepo(VCSRepository repo) {
		this.repo = repo;
	}
}