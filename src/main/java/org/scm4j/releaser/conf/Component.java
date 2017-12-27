package org.scm4j.releaser.conf;

import org.scm4j.commons.Version;
import org.scm4j.commons.coords.Coords;
import org.scm4j.commons.coords.CoordsGradle;

import java.util.Objects;

public class Component {
	private final Coords coords;

	public Component(String coords) {
		this.coords = new CoordsGradle(coords);
	}

	public Coords getCoords() {
		return coords;
	}
	
	public String getCoordsNoVersion() {
		return coords.toString("");
	}

	public String getName() {
		return coords.getName().trim();
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

		return Objects.equals(coords, component.coords);
	}

	@Override
	public int hashCode() {
		return coords == null ? 0 : coords.hashCode();
	}

	public String getCoordsNoComment() {
		return coords.toStringNoComment();
	}

}