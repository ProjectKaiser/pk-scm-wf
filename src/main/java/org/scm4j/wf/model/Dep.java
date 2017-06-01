package org.scm4j.wf.model;

import java.util.Map;

public class Dep {
	private String name;
	private String resolvedVersion;
	private String ver;
	private String lastVerCommit;
	private Boolean isManaged;
	private VCSRepository vcsRepository;

	public Dep() {

	}

	public Boolean getIsManaged() {
		return isManaged;
	}

	public void setIsManaged(Boolean isManaged) {
		this.isManaged = isManaged;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getResolvedVersion() {
		return resolvedVersion;
	}

	public void setResolvedVersion(String resolvedVersion) {
		this.resolvedVersion = resolvedVersion;
	}

	public String getVer() {
		return ver;
	}

	public void setVer(String ver) {
		this.ver = ver;
	}

	public String getLastVerCommit() {
		return lastVerCommit;
	}

	public void setLastVerCommit(String lastVerCommit) {
		this.lastVerCommit = lastVerCommit;
	}

	public VCSRepository getVcsRepository() {
		return vcsRepository;
	}

	public void setVcsRepository(VCSRepository vcsRepository) {
		this.vcsRepository = vcsRepository;
	}

	@Override
	public String toString() {
		return "Dep [name=" + name + ", version=" + ver + "]";
	}
	
	public static Dep fromCoords(String coords) {
		String[] parts = coords.split(":");
		if (parts.length < 2) {
			throw new IllegalArgumentException("wrong mdep coords: " + coords);
		}
		Dep dep = new Dep();
		if (parts.length == 2) {
			dep.setName(coords);
		} else {
			dep.setName(coords.replace(":" + parts[2], ""));
			dep.setVer(parts[2]);
		}
		return dep;
	}
	
	public static Dep fromCoords(String coords, VCSRepository repo) {
		Dep dep = fromCoords(coords);
		dep.setVcsRepository(repo);
		return dep;
	}

	public static Dep fromCoords(String coords, Map<String, VCSRepository> vcsRepos) {
		Dep dep = fromCoords(coords);
		dep.setVcsRepository(vcsRepos.get(dep.getName()));
		return dep;
	}

	public String toCoords() {
		return name + ":" + ver;
	}

}