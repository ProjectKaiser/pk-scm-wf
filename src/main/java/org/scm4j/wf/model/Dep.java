package org.scm4j.wf.model;

import java.util.Map;

import org.scm4j.wf.conf.DepCoords;

public class Dep extends DepCoords {
	private Boolean isManaged;
	private VCSRepository vcsRepository;

	public Dep() {
		super();
	}

	public Boolean getIsManaged() {
		return isManaged;
	}

	public void setIsManaged(Boolean isManaged) {
		this.isManaged = isManaged;
	}

	public VCSRepository getVcsRepository() {
		return vcsRepository;
	}

	public void setVcsRepository(VCSRepository vcsRepository) {
		this.vcsRepository = vcsRepository;
	}

	public Dep(String coords, VCSRepository repo) {
		super(coords);
		vcsRepository = repo;
	}

	public Dep(String coords, Map<String, VCSRepository> vcsRepos) {
		super(coords);
		vcsRepository = vcsRepos.get(nameStr);
	}
}