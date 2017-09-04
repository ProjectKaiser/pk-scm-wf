package org.scm4j.wf.conf;

import org.scm4j.vcs.api.IVCS;

public class Component {
	private final VCSRepository vcsRepository;
	private final Coords coords;

	public VCSRepository getVcsRepository() {
		return vcsRepository;
	}

	public Component(String coordsStr, VCSRepository repo) {
		coords = new Coords(coordsStr);
		vcsRepository = repo;
	}
	
	public IVCS getVCS() {
		return vcsRepository.getVcs();
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
		return new Component(coords.toString(versionStr), vcsRepository);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((coords == null) ? 0 : coords.hashCode());
		result = prime * result + ((vcsRepository == null) ? 0 : vcsRepository.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Component other = (Component) obj;
		if (coords == null) {
			if (other.coords != null)
				return false;
		} else if (!coords.equals(other.coords))
			return false;
		if (vcsRepository == null) {
			if (other.vcsRepository != null)
				return false;
		} else if (!vcsRepository.equals(other.vcsRepository))
			return false;
		return true;
	}
	
}