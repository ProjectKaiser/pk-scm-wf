package org.scm4j.wf.scmactions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.workingcopy.IVCSLockedWorkingCopy;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.DevelopBranch;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.branch.ReleaseBranchStatus;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.Option;
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.exceptions.EBuilder;

	
public class SCMActionBuild extends ActionAbstract {
	private final ReleaseReason reason;
	private final Version targetVersion;

	public SCMActionBuild(Component comp, List<IAction> childActions, ReleaseReason reason, Version targetVersion, List<Option> options) {
		super(comp, childActions, options);
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
			DevelopBranch db = new DevelopBranch(comp);
			ReleaseBranch rb = db.getCurrentReleaseBranch(repos);
			ReleaseBranchStatus rbs = rb.getStatus();
			if (rbs == ReleaseBranchStatus.ACTUAL) {
				progress.reportStatus("version " + rb.getVersion().toString() + " already built ");
				return;
			}
			
			progress.reportStatus("target version to build: " + targetVersion);
			
			for (IAction action : childActions) {
				try (IProgress nestedProgress = progress.createNestedProgress(action.toString())) {
					action.execute(nestedProgress);
				}
			}
			
			if (comp.getVcsRepository().getBuilder() == null) {
				throw new EBuilder("no builder defined");
			} 
			
			VCSCommit headCommit = vcs.getHeadCommit(rb.getReleaseBranchName());
				
			try (IVCSLockedWorkingCopy lwc = vcs.getWorkspace().getVCSRepositoryWorkspace(vcs.getRepoUrl()).getVCSLockedWorkingCopy()) {
				lwc.setCorrupted(true); // use lwc only once
				progress.reportStatus(String.format("checking out %s on revision %s into %s", getName(), headCommit.getRevision(), lwc.getFolder().getPath()));
				vcs.checkout(rb.getReleaseBranchName(), lwc.getFolder().getPath(), headCommit.getRevision());
				comp.getVcsRepository().getBuilder().build(comp, lwc.getFolder(), progress);
			}
			
			if (options.contains(Option.DELAYED_TAG)) {
				CommitsFile commitsFile = new CommitsFile();
				commitsFile.writeCompRevision(comp.getName(), headCommit.getRevision());
				progress.reportStatus("build commit " + headCommit.getRevision() + " is saved for delayed tagging");
			} else {
				String releaseBranchName = rb.getReleaseBranchName();
				String tagName = rb.getVersion().toString();
				String tagMessage = tagName + " release"; 
				VCSTag tag = vcs.createTag(releaseBranchName, tagName, tagMessage, headCommit.getRevision());
				progress.reportStatus("head of \"" + releaseBranchName + "\" tagged: " + tag.toString());
			}

			VCSCommit builtCommit = vcs.setFileContent(rb.getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME, targetVersion.toNextPatch().toReleaseString(), 
					LogTag.SCM_VER + " " + targetVersion.toNextPatch().toReleaseString());
			
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
