package org.scm4j.wf.scmactions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSLockedWorkingCopy;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.DevelopBranch;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.branch.ReleaseBranchStatus;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.exceptions.EBuilder;

	
public class SCMActionBuild extends ActionAbstract {
	private final ReleaseReason reason;
	private final Version targetVersion;

	public SCMActionBuild(Component comp, List<IAction> childActions, ReleaseReason reason, Version targetVersion) {
		super(comp, childActions);
		this.reason = reason;
		this.targetVersion = targetVersion;
	}

	public Version getTargetVersion() {
		return targetVersion;
	}

	public ReleaseReason getReason() {
		return reason;
	}

	@Override
	public void execute(IProgress progress) {
		try {
			
			IVCS vcs = getVCS();
			VCSRepositories repos = VCSRepositories.loadVCSRepositories();
			DevelopBranch db = new DevelopBranch(comp);
			ReleaseBranch rb = db.getCurrentReleaseBranch(repos);
			ReleaseBranchStatus rbs = rb.getStatus();
			if (rbs == ReleaseBranchStatus.BUILT || rbs == ReleaseBranchStatus.TAGGED) {
				progress.reportStatus("version " + rb.getTargetVersion().toString() + " already built");
				return;
				//return new ActionResultVersion(comp.getName(), rb.getTargetVersion().toString(), true, rb.getReleaseBranchName());
			}
			
			progress.reportStatus("target version to build: " + targetVersion);
			
			for (IAction action : childActions) {
				try (IProgress nestedProgress = progress.createNestedProgress(action.toString())) {
					action.execute(nestedProgress);
				}
			}
			
			
			if (comp.getVcsRepository().getBuilder() == null) {
				throw new EBuilder("no builder defined");
			} else {
				try (IVCSLockedWorkingCopy lwc = vcs.getWorkspace().getVCSRepositoryWorkspace(vcs.getRepoUrl()).getVCSLockedWorkingCopy()) {
					lwc.setCorrupted(true); // use lwc only once
					progress.reportStatus(String.format("checking out %s into %s", getName(), lwc.getFolder().getPath()));
					vcs.checkout(rb.getReleaseBranchName(), lwc.getFolder().getPath());
					comp.getVcsRepository().getBuilder().build(comp, lwc.getFolder(), progress);
				}
			}
			
			vcs.setFileContent(rb.getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME, targetVersion.toString(), LogTag.SCM_BUILT + " " + targetVersion.toString());
			
			progress.reportStatus(comp.getName() + " " + targetVersion.toString() + " is built in " + rb.getReleaseBranchName());
		} catch (Throwable t) {
			progress.error("execution error: " + t.toString() + ": " + t.getMessage());
			throw new RuntimeException(t);
		} 
	}

	@Override
	public String toString() {
		return "build " + comp.getCoords().toString() + ", targetVersion=" + targetVersion.toString() + ", " + reason.toString();
	}

}
