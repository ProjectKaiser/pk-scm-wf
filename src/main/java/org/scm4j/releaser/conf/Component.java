package org.scm4j.releaser.conf;

import org.scm4j.commons.Version;
import org.scm4j.commons.coords.Coords;
import org.scm4j.commons.coords.CoordsGradle;
import org.scm4j.vcs.api.IVCS;

import java.util.Objects;

public class Component {
	private final Coords coords;
	private final VCSRepository repo;
	
	public VCSRepository getVcsRepository() {
		return repo;
	}
	
	public Component(String coords, VCSRepositoryFactory repoFactory) {
		this.coords = new CoordsGradle(coords);
		repo = repoFactory.getVCSRepository(this.coords.getName());
	}

	public Component(String coords) {
		this.coords = new CoordsGradle(coords);
		repo = null;
	}

	public Component(String coordsStr, VCSRepository repo) {
		coords = new CoordsGradle(coordsStr);
		this.repo = repo;
	}
	
	public IVCS getVCS() {
		return getVcsRepository().getVcs();
	}
	
	public Coords getCoords() {
		return coords;
	}
	
	public String getCoordsNoVersion() {
		return coords.toString("");
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
		return new Component(coords.toString(newVersion), repo);
	}
	
	public Component clone(Version newVersion) {
		return clone(newVersion.toString());
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Component component = (Component) o;

		return Objects.equals(coords, component.coords);
	}

	@Override
	public int hashCode() {
		return coords == null ? 0 : coords.hashCode();
	}

	public String getCoordsNoComment() {
		return coords.toStringNoComment();
	}

	public String getUrl() {
		return getVcsRepository().getUrl();
	}
}