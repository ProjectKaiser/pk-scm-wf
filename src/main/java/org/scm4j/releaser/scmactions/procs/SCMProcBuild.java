package org.scm4j.releaser.scmactions.procs;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.CalculatedResult;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.*;
import org.scm4j.releaser.exceptions.ENoBuilder;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;

import java.io.File;
import java.nio.file.Files;

public class SCMProcBuild implements ISCMProc {
	
	private final ReleaseBranch rb;
	private final IVCS vcs;
	private final Component comp;
	private final CalculatedResult calculatedResult;
	private final boolean delayedTag;
 
	public SCMProcBuild(ReleaseBranch rb, Component comp, CalculatedResult calculatedResult, boolean delayedTag) {
		this.rb = rb;
		this.comp = comp;
		this.calculatedResult = calculatedResult;
		this.delayedTag = delayedTag;
		vcs = comp.getVCS();
	}

	@Override
	public void execute(IProgress progress) {
		VCSCommit headCommit = vcs.getHeadCommit(rb.getName());
		if (headCommit == null) {
			throw new EReleaserException("branch does not exist: " + rb.getName());
		}
		
		if (comp.getVcsRepository().getBuilder() == null) {
			throw new ENoBuilder(comp);
		}
		
		build(progress, headCommit);
		
		tagBuild(progress, headCommit);
		
		Version newVersion = raisePatchVersion(progress);
		
		calculatedResult.replaceReleaseBranch(comp, new ReleaseBranch(comp, newVersion, true));
		
		progress.reportStatus(comp.getName() + " " + rb.getVersion().toString() + " is built in " + rb.getName());

	}
	
	@SneakyThrows
	private void build(IProgress progress, VCSCommit headCommit) {
		File buildDir = rb.getBuildDir();
		if (buildDir.exists()) {
			FileUtils.deleteDirectory(buildDir);
		}
		Files.createDirectories(buildDir.toPath());		
		Utils.reportDuration(() -> vcs.checkout(rb.getName(), buildDir.getPath(), headCommit.getRevision()),
				String.format("check out %s on revision %s into %s", comp.getName(), headCommit.getRevision(), buildDir.getPath()), null, progress);
		comp.getVcsRepository().getBuilder().build(comp, buildDir, progress);
	}

	@SneakyThrows
	private void tagBuild(IProgress progress, VCSCommit headCommit) {
		if (delayedTag) {
			DelayedTagsFile delayedTagsFile = new DelayedTagsFile();
			delayedTagsFile.writeUrlRevision(comp.getVcsRepository().getUrl(), headCommit.getRevision());
			progress.reportStatus("build commit " + headCommit.getRevision() + " is saved for delayed tagging");
		} else {
			String releaseBranchName = rb.getName();
			TagDesc tagDesc = SCMReleaser.getTagDesc(rb.getVersion().toString());
			Utils.reportDuration(() -> vcs.createTag(releaseBranchName, tagDesc.getName(), tagDesc.getMessage(), headCommit.getRevision()),
					String.format("tag head of %s: %s", releaseBranchName, tagDesc.getName()), null, progress);
		}
	}

	private Version raisePatchVersion(IProgress progress) {
		Version nextPatchVersion = rb.getVersion().toNextPatch();
		Utils.reportDuration(() -> vcs.setFileContent(rb.getName(), SCMReleaser.VER_FILE_NAME, nextPatchVersion.toString(),
				LogTag.SCM_VER + " " + nextPatchVersion),
				"bump patch version in release branch: " + nextPatchVersion, null, progress);
		return nextPatchVersion;
	}

}
