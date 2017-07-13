package org.scm4j.wf.conf;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.wf.SCMWorkflow;

public class Dep extends DepCoords {
	private Boolean isManaged;
	private VCSRepository vcsRepository;

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
	
	public Dep(String coords, VCSRepositories repos) {
		super(coords);
		vcsRepository = repos.get(getName());
		if (vcsRepository == null) {
			throw new IllegalArgumentException("VCSRepository is not found by name " + getName());
		}
		if (getVersion().isEmpty()) {
			setVersion(getActualVersion());
		}
	}
	
	public Version getActualVersion() {
		IVCS vcs = vcsRepository.getVcs();
		String verFileContent = vcs.getFileContent(vcsRepository.getDevBranch(), SCMWorkflow.VER_FILE_NAME);
		return new Version(verFileContent.trim());
	}
	
	public String getReleaseBranchName() {
		return vcsRepository.getReleaseBranchPrefix() + getVersion().toReleaseString();
	}

	public String getPreviousMinorReleaseBranchName() {
		return vcsRepository.getReleaseBranchPrefix() + getVersion().toPreviousMinorRelease();
	}
}