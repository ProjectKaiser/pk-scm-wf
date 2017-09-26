package org.scm4j.releaser.scmactions;

import java.io.IOException;
import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.Option;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.exceptions.EBuilder;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.exceptions.EVCSTagExists;
import org.scm4j.vcs.api.workingcopy.IVCSLockedWorkingCopy;

	
public class SCMActionBuild extends ActionAbstract {

	private final IVCS vcs;
	private final ReleaseBranch crb;

	public SCMActionBuild(ReleaseBranch crb, List<IAction> childActions, BuildStatus mbs) {
		super(crb.getComponent(), childActions);
		this.crb = crb;
		vcs = getVCS();
	}

	@Override
	public void execute(IProgress progress) {
		if (isUrlProcessed(comp.getVcsRepository().getUrl())) {
			progress.reportStatus("already executed");
			return;
		}
		try {
			super.executeChilds(progress);

			VCSCommit headCommit = vcs.getHeadCommit(crb.getName());
			
			progress.reportStatus("target version to build: " + crb.getVersion());
			
			if (comp.getVcsRepository().getBuilder() == null) {
				throw new EBuilder("no builder defined for " + comp);
			} 

			build(progress, headCommit);
			
			tagBuild(progress, headCommit);
			
			raisePatchVersion(progress);
			
			progress.reportStatus(comp.getName() + " " + crb.getVersion().toString() + " is built in " + crb.getName());
			addProcessedUrl(comp.getVcsRepository().getUrl());
		} catch (EReleaserException e) {
			throw e;
		} catch (Exception e) {
			progress.error("execution error: " + e.toString());
			throw new EReleaserException(e);
		}
	}

	private void build(IProgress progress, VCSCommit headCommit) throws Exception, IOException {
		try (IVCSLockedWorkingCopy lwc = vcs.getWorkspace().getVCSRepositoryWorkspace(vcs.getRepoUrl()).getVCSLockedWorkingCopyTemp()) {
			progress.reportStatus(String.format("checking out %s on revision %s into %s", getName(), headCommit.getRevision(), lwc.getFolder().getPath()));
			vcs.checkout(crb.getName(), lwc.getFolder().getPath(), headCommit.getRevision());
			comp.getVcsRepository().getBuilder().build(comp, lwc.getFolder(), progress);
		}
	}

	private void tagBuild(IProgress progress, VCSCommit headCommit) throws IOException {
		if (Options.hasOption(Option.DELAYED_TAG)) {
			DelayedTagsFile delayedTagsFile = new DelayedTagsFile();
			delayedTagsFile.writeUrlRevision(comp.getVcsRepository().getUrl(), headCommit.getRevision());
			progress.reportStatus("build commit " + headCommit.getRevision() + " is saved for delayed tagging");
		} else {
			String releaseBranchName = crb.getName();
			TagDesc tagDesc = SCMReleaser.getTagDesc(crb.getHeadVersion().toString());
			try {
				VCSTag tag = vcs.createTag(releaseBranchName, tagDesc.getName(), tagDesc.getMessage(), headCommit.getRevision());
				progress.reportStatus("head of \"" + releaseBranchName + "\" tagged: " + tag.toString());
			} catch (EVCSTagExists e) {
				progress.reportStatus("head of \"" + releaseBranchName + "\" is tagged already: " + tagDesc.getName());
			}
		}
	}

	private void raisePatchVersion(IProgress progress) {
		Version nextPatchVersion = crb.getHeadVersion().toNextPatch();
		vcs.setFileContent(crb.getName(), SCMReleaser.VER_FILE_NAME, nextPatchVersion.toString(),
				LogTag.SCM_VER + " " + nextPatchVersion);
		progress.reportStatus("patch version is raised in release branch: " + nextPatchVersion);
	}

	@Override
	public String toString() {
		return "build " + comp.getCoords().toString() + ", targetVersion: " + crb.getVersion().toString();
	}

	public Version getTargetVersion() {
		return crb.getVersion();
	}
}
