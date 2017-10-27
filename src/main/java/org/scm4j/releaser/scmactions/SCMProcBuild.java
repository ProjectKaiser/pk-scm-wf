package org.scm4j.releaser.scmactions;

import java.io.File;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.Option;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;

import lombok.SneakyThrows;

public class SCMProcBuild implements ISCMProc {
	
	private final ReleaseBranch rb;
	private final IVCS vcs;
	private final Component comp;
 
	public SCMProcBuild(ReleaseBranch rb) {
		this.rb = rb;
		comp = rb.getComponent();
		vcs = rb.getComponent().getVCS();
	}

	@Override
	public void execute(IProgress progress) {
		VCSCommit headCommit = vcs.getHeadCommit(rb.getName());
		if (headCommit == null) {
			throw new EReleaserException("branch does not exist: " + rb.getName());
		}
		
		build(progress, headCommit);
		
		tagBuild(progress, headCommit);
		
		raisePatchVersion(progress);
		
		progress.reportStatus(comp.getName() + " " + rb.getVersion().toString() + " is built in " + rb.getName());

	}
	
	@SneakyThrows
	private void build(IProgress progress, VCSCommit headCommit) {
		File buildDir = rb.getBuildDir();
		if (buildDir.exists()) {
			FileUtils.deleteDirectory(buildDir);
		}
		Files.createDirectories(buildDir.toPath());		
		progress.startTrace(String.format("checking out %s on revision %s into %s...", comp.getName(), headCommit.getRevision(), buildDir.getPath()));
		vcs.checkout(rb.getName(), buildDir.getPath(), headCommit.getRevision());
		progress.endTrace("done");
		comp.getVcsRepository().getBuilder().build(comp, buildDir, progress);
	}

	@SneakyThrows
	private void tagBuild(IProgress progress, VCSCommit headCommit) {
		if (Options.hasOption(Option.DELAYED_TAG)) {
			DelayedTagsFile delayedTagsFile = new DelayedTagsFile();
			delayedTagsFile.writeUrlRevision(comp.getVcsRepository().getUrl(), headCommit.getRevision());
			progress.reportStatus("build commit " + headCommit.getRevision() + " is saved for delayed tagging");
		} else {
			String releaseBranchName = rb.getName();
			TagDesc tagDesc = SCMReleaser.getTagDesc(rb.getVersion().toString());
			progress.startTrace(String.format("tagging head of %s: %s...", releaseBranchName, tagDesc.getName()));
			vcs.createTag(releaseBranchName, tagDesc.getName(), tagDesc.getMessage(), headCommit.getRevision());
			progress.endTrace("done");
		}
	}

	private void raisePatchVersion(IProgress progress) {
		Version nextPatchVersion = rb.getVersion().toNextPatch();
		progress.startTrace("bumping patch version in release branch: " + nextPatchVersion);
		vcs.setFileContent(rb.getName(), SCMReleaser.VER_FILE_NAME, nextPatchVersion.toString(),
				LogTag.SCM_VER + " " + nextPatchVersion);
		progress.endTrace("done");
	}

}
