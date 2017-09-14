package org.scm4j.wf.scmactions;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;
import org.scm4j.vcs.api.workingcopy.IVCSLockedWorkingCopy;
import org.scm4j.wf.LogTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.branch.ReleaseBranchStatus;
import org.scm4j.wf.conf.*;
import org.scm4j.wf.exceptions.EBuilder;

import java.util.List;

	
public class SCMActionBuild extends ActionAbstract {
	private final ReleaseReason reason;
	private final Version targetVersion;

	public SCMActionBuild(Component comp, List<IAction> childActions, ReleaseReason reason, Version targetVersion, List<Option> options) {
		super(comp, childActions, options);
		this.reason = reason;
		this.targetVersion = targetVersion;
	}

	public ReleaseReason getReason() {
		return reason;
	}

	@Override
	public void execute(IProgress progress) {
		try {
			IVCS vcs = getVCS();
			ReleaseBranch rb = new ReleaseBranch(comp);
			ReleaseBranchStatus rbs = rb.getStatus();
			if (rbs == ReleaseBranchStatus.ACTUAL) {
				progress.reportStatus("version " + rb.getVersion().toString() + " already built ");
				return;
			}
			
			VCSCommit headCommit = vcs.getHeadCommit(rb.getName());
			
			// need to check if we are built already with delayed tag
			DelayedTagsFile cf = new DelayedTagsFile();
			String delayedTagRevision = cf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
			if (delayedTagRevision != null) {
				List<VCSCommit> commits = vcs.getCommitsRange(rb.getName(), null, WalkDirection.DESC, 2);
				if (commits.size() == 2) {
					if (commits.get(1).getRevision().equals(delayedTagRevision)) {
						progress.reportStatus(String.format("version %s already built with delayed tag on revision %s", rb.getVersion().toString(), delayedTagRevision));
						return;
					}
				}
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
			
			
				
			try (IVCSLockedWorkingCopy lwc = vcs.getWorkspace().getVCSRepositoryWorkspace(vcs.getRepoUrl()).getVCSLockedWorkingCopy()) {
				lwc.setCorrupted(true); // use lwc only once
				progress.reportStatus(String.format("checking out %s on revision %s into %s", getName(), headCommit.getRevision(), lwc.getFolder().getPath()));
				vcs.checkout(rb.getName(), lwc.getFolder().getPath(), headCommit.getRevision());
				comp.getVcsRepository().getBuilder().build(comp, lwc.getFolder(), progress);
			}
			
			if (options.contains(Option.DELAYED_TAG)) {
				DelayedTagsFile delayedTagsFile = new DelayedTagsFile();
				delayedTagsFile.writeUrlRevision(comp.getVcsRepository().getUrl(), headCommit.getRevision());
				progress.reportStatus("build commit " + headCommit.getRevision() + " is saved for delayed tagging");
			} else {
				String releaseBranchName = rb.getName();
				TagDesc tagDesc = SCMWorkflow.getTagDesc(rb.getVersion().toString());
				VCSTag tag = vcs.createTag(releaseBranchName, tagDesc.getName(), tagDesc.getMessage(), headCommit.getRevision());
				progress.reportStatus("head of \"" + releaseBranchName + "\" tagged: " + tag.toString());
			}

			vcs.setFileContent(rb.getName(), SCMWorkflow.VER_FILE_NAME, targetVersion.toNextPatch().toReleaseString(), 
					LogTag.SCM_VER + " " + targetVersion.toNextPatch().toReleaseString());
			
			progress.reportStatus(comp.getName() + " " + targetVersion.toString() + " is built in " + rb.getName());
		} catch (Throwable t) {
			progress.error("execution error: " + t.toString() + ": " + t.getMessage());
			throw new RuntimeException(t);
		}
	}

	@Override
	public String toString() {
		return "build " + comp.getCoords().toString() + ", targetVersion: " + targetVersion.toString() + ", " + reason.toString();
	}
	
	public Version getTargetVersion() {
		return targetVersion;
	}
}
