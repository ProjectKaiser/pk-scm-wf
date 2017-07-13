package org.scm4j.wf.branchstatus;

public class BranchStatuses {

	private DevelopBranchStatus developStatus;
	private ReleaseBranchStatus releaseStatus;

	public DevelopBranchStatus getDevelopStatus() {
		return developStatus;
	}

	public ReleaseBranchStatus getReleaseStatus() {
		return releaseStatus;
	}

	public void setDevelopStatus(DevelopBranchStatus developStatus) {
		this.developStatus = developStatus;
	}

	public void setReleaseStatus(ReleaseBranchStatus releaseStatus) {
		this.releaseStatus = releaseStatus;
	}

	@Override
	public String toString() {
		return "BranchStatuses [developStatus=" + developStatus + ", releaseStatus=" + releaseStatus + "]";
	}
}
