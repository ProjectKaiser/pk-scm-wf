package org.scm4j.releaser.conf;

import org.scm4j.commons.Coords;
import org.scm4j.commons.Version;
import org.scm4j.vcs.api.IVCS;

public class Component {
	private final boolean isProduct;
	private final Coords coords;

	public VCSRepository getVcsRepository() {
		return VCSRepositories.getDefault().getByName(coords.getName());
	}

	public Component(String coordsStr, boolean isProduct) {
		coords = new Coords(coordsStr);
		this.isProduct = isProduct;
	}
	
	public Component(String coordsStr) {
		coords = new Coords(coordsStr);
		isProduct = false;
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
		return coords.toString() + (isProduct ? ", PRODUCT" : "");
	}
	
	public Version getVersion() {
		return coords.getVersion();
	}

	public Component clone(String newVersion) {
		return new Component(coords.toString(newVersion), isProduct);
	}
	
	public Component clone(Version newVersion) {
		return clone(newVersion.toString());
	}
	
	public Component cloneProduct(Version newVersion) {
		return new Component(coords.toString(newVersion.toString()), true);
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

	public boolean isProduct() {
		return isProduct;
	}
}