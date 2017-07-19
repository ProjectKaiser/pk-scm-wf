package org.scm4j.wf.conf;

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
	
	public Component(String coordsStr, VCSRepositories repos) {
		this(coordsStr, repos.getByCoords(coordsStr));
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
	
}