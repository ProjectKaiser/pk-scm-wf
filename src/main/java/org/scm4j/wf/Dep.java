package org.scm4j.wf;

public class Dep {
	private String name;
	private String resolvedVersion;
	private String version;
	private String lastBuildCommitId;
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

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getLastBuildCommitId() {
		return lastBuildCommitId;
	}

	public void setLastBuildCommitId(String lastBuildCommitId) {
		this.lastBuildCommitId = lastBuildCommitId;
	}

	public VCSRepository getVcsRepository() {
		return vcsRepository;
	}

	public void setVcsRepository(VCSRepository vcsRepository) {
		this.vcsRepository = vcsRepository;
	}

	@Override
	public String toString() {
		return "Dep [name=" + name + ", resolvedVersion=" + resolvedVersion + ", version=" + version + "]";
	}

}