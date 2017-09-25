package org.scm4j.releaser.scmactions;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.*;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.CurrentReleaseBranch;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.Option;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.exceptions.EBuilder;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.exceptions.EVCSTagExists;
import org.scm4j.vcs.api.workingcopy.IVCSLockedWorkingCopy;

import java.io.IOException;
import java.util.List;

	
public class SCMActionBuild extends ActionAbstract {

	private final IVCS vcs;
	private final CurrentReleaseBranch crb;

	public SCMActionBuild(CurrentReleaseBranch crb, List<IAction> childActions, MinorBuildStatus mbs) {
		super(crb.getComponent(), childActions);
		this.crb = crb;
		vcs = getVCS();
	}

	@Override
	public void execute(IProgress progress) {
		if (isCompProcessed(comp)) {
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
		} catch (Throwable t) {
			progress.error("execution error: " + t.toString() + ": " + t.getMessage());
			throw new RuntimeException(t);
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
			TagDesc tagDesc = SCMReleaser.getTagDesc(crb.getVersion().toString());
			try {
				VCSTag tag = vcs.createTag(releaseBranchName, tagDesc.getName(), tagDesc.getMessage(), headCommit.getRevision());
				progress.reportStatus("head of \"" + releaseBranchName + "\" tagged: " + tag.toString());
			} catch (EVCSTagExists e) {
				progress.reportStatus("head of \"" + releaseBranchName + "\" is tagged already: " + tagDesc.getName());
			}
		}
	}

	private void raisePatchVersion(IProgress progress) {
		Version nextPatchVersion = crb.getVersion().toNextPatch();
		vcs.setFileContent(crb.getName(), SCMReleaser.VER_FILE_NAME, nextPatchVersion.toString(),
				LogTag.SCM_VER + " " + crb.getVersion().toNextPatch().toReleaseString());
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
