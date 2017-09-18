package org.scm4j.releaser.scmactions;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.branch.ReleaseBranchStatus;
import org.scm4j.releaser.conf.*;
import org.scm4j.releaser.exceptions.EBuilder;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;
import org.scm4j.vcs.api.exceptions.EVCSTagExists;
import org.scm4j.vcs.api.workingcopy.IVCSLockedWorkingCopy;

import java.io.IOException;
import java.util.List;

	
public class SCMActionBuild extends ActionAbstract {
	private final ReleaseReason reason;
	private final ReleaseBranch rb;
	private final IVCS vcs;

	public SCMActionBuild(Component comp, ReleaseBranch rb, List<IAction> childActions, ReleaseReason reason, Version targetVersion, List<Option> options) {
		super(comp, childActions, options);
		this.reason = reason;
		this.rb = rb;
		vcs = getVCS();
	}

	public ReleaseReason getReason() {
		return reason;
	}

	@Override
	public void execute(IProgress progress) {
		try {
			ReleaseBranchStatus rbs = rb.getStatus();
			if (rbs == ReleaseBranchStatus.ACTUAL) {
				progress.reportStatus("version " + rb.getVersion().toString() + " already built");
				return;
			}
			
			VCSCommit headCommit = vcs.getHeadCommit(rb.getName());
			
			String delayedTagRevision = getDelayedTagRevision();
			if (delayedTagRevision != null) {
				progress.reportStatus(String.format("version %s already built with delayed tag on revision %s", rb.getVersion().toString(), delayedTagRevision));
				return;
			}
			
			progress.reportStatus("target version to build: " + rb.getVersion());
			
			for (IAction action : childActions) {
				try (IProgress nestedProgress = progress.createNestedProgress(action.toString())) {
					action.execute(nestedProgress);
				}
			}
			
			if (comp.getVcsRepository().getBuilder() == null) {
				throw new EBuilder("no builder defined for " + comp);
			} 

			build(progress, headCommit);
			
			tagBuild(progress, headCommit);

			raisePatchVersion(progress);
			
			progress.reportStatus(comp.getName() + " " + rb.getVersion().toString() + " is built in " + rb.getName());
		} catch (Throwable t) {
			progress.error("execution error: " + t.toString() + ": " + t.getMessage());
			throw new RuntimeException(t);
		}
	}

	private String getDelayedTagRevision() {
		DelayedTagsFile cf = new DelayedTagsFile();
		String delayedTagRevision = cf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
		if (delayedTagRevision != null) {
			List<VCSCommit> commits = vcs.getCommitsRange(rb.getName(), null, WalkDirection.DESC, 2);
			if (commits.size() == 2) {
				if (commits.get(1).getRevision().equals(delayedTagRevision)) {
					return delayedTagRevision;
				}
			}
		}
		return null;
	}

	private void build(IProgress progress, VCSCommit headCommit) throws Exception, IOException {
		try (IVCSLockedWorkingCopy lwc = vcs.getWorkspace().getVCSRepositoryWorkspace(vcs.getRepoUrl()).getVCSLockedWorkingCopy()) {
			lwc.setCorrupted(true); // use lwc only once for building
			progress.reportStatus(String.format("checking out %s on revision %s into %s", getName(), headCommit.getRevision(), lwc.getFolder().getPath()));
			vcs.checkout(rb.getName(), lwc.getFolder().getPath(), headCommit.getRevision());
			comp.getVcsRepository().getBuilder().build(comp, lwc.getFolder(), progress);
		}
	}

	private void tagBuild(IProgress progress, VCSCommit headCommit) throws IOException {
		if (options.contains(Option.DELAYED_TAG)) {
			DelayedTagsFile delayedTagsFile = new DelayedTagsFile();
			delayedTagsFile.writeUrlRevision(comp.getVcsRepository().getUrl(), headCommit.getRevision());
			progress.reportStatus("build commit " + headCommit.getRevision() + " is saved for delayed tagging");
		} else {
			String releaseBranchName = rb.getName();
			TagDesc tagDesc = SCMReleaser.getTagDesc(rb.getVersion().toString());
			try {
				VCSTag tag = vcs.createTag(releaseBranchName, tagDesc.getName(), tagDesc.getMessage(), headCommit.getRevision());
				progress.reportStatus("head of \"" + releaseBranchName + "\" tagged: " + tag.toString());
			} catch (EVCSTagExists e) {
				progress.reportStatus("head of \"" + releaseBranchName + "\" is tagged already: " + tagDesc.getName());
			}
		}
	}

	private void raisePatchVersion(IProgress progress) {
		Version nextPatchVersion = rb.getVersion().toNextPatch().toRelease();
		Version headVersion = rb.getHeadVersion();
		if (headVersion.equals(nextPatchVersion) || headVersion.isGreaterThan(nextPatchVersion)) {
			progress.reportStatus("patch version is raised already: " + headVersion);
		} else {
			vcs.setFileContent(rb.getName(), SCMReleaser.VER_FILE_NAME, nextPatchVersion.toString(),
					LogTag.SCM_VER + " " + rb.getVersion().toNextPatch().toReleaseString());
			progress.reportStatus("patch version is raised in release branch: " + nextPatchVersion);
		}
	}

	@Override
	public String toString() {
		return "build " + comp.getCoords().toString() + ", targetVersion: " + rb.getVersion().toString() + ", " + reason.toString();
	}

	public Version getTargetVersion() {
		return rb.getVersion();
	}
}
