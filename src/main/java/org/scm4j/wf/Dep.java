package org.scm4j.wf;

import java.util.Map;

public class Dep {
	private String name;
	private String resolvedVersion;
	private String ver;
	private String lastVerCommit;
	// private Artifactory repository;
	private Boolean isManaged;
	private VCSRepository vcsRepository;
	private String masterBranchName;
	
	public String getMasterBranchName() {
		return masterBranchName;
	}

	public void setMasterBranchName(String masterBranchName) {
		this.masterBranchName = masterBranchName;
	}

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
	
	public static Dep fromName(String depName, Map<String, VCSRepository> vcsRepos) {
		Dep res = new Dep();
		res.setName(depName);
		res.setVcsRepository(vcsRepos.get(depName));
		return res;
	}

}