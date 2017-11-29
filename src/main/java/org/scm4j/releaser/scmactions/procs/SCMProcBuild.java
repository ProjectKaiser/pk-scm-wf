package org.scm4j.releaser.scmactions.procs;

import java.io.File;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.Option;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.exceptions.ENoBuilder;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;

import lombok.SneakyThrows;

public class SCMProcBuild implements ISCMProc {
	
	private final IVCS vcs;
	private final Component comp;
	private final String releaseBranchName;
	private final Version versionToBuild;
 
	public SCMProcBuild(Component comp, CachedStatuses cache) {
		this.comp = comp;
		vcs = comp.getVCS();
		releaseBranchName = Utils.getReleaseBranchName(comp, cache.get(comp.getUrl()).getLatestVersion());
		versionToBuild = cache.get(comp.getUrl()).getLatestVersion();
	}

	@Override
	public void execute(IProgress progress) {
		VCSCommit headCommit = vcs.getHeadCommit(releaseBranchName);
		if (headCommit == null) {
			throw new EReleaserException("branch does not exist: " + releaseBranchName);
		}
		
		if (comp.getVcsRepository().getBuilder() == null) {
			throw new ENoBuilder(comp);
		}
		
		build(progress, headCommit);
		
		tagBuild(progress, headCommit);
		
		raisePatchVersion(progress);
		
		// for what?
		//calculatedResult.replaceReleaseBranch(comp, new ReleaseBranch(comp, newVersion, true));
		
		progress.reportStatus(comp.getName() + " " + versionToBuild + " is built in " + releaseBranchName);

	}
	
	@SneakyThrows
	private void build(IProgress progress, VCSCommit headCommit) {
		File buildDir = Utils.getBuildDir(comp, versionToBuild);
		if (buildDir.exists()) {
			FileUtils.deleteDirectory(buildDir);
		}
		Files.createDirectories(buildDir.toPath());		
		Utils.reportDuration(() -> vcs.checkout(releaseBranchName, buildDir.getPath(), headCommit.getRevision()),
				String.format("check out %s on revision %s into %s", comp.getName(), headCommit.getRevision(), buildDir.getPath()), null, progress);
		comp.getVcsRepository().getBuilder().build(comp, buildDir, progress);
	}

	@SneakyThrows
	private void tagBuild(IProgress progress, VCSCommit headCommit) {
		if (Options.hasOption(Option.DELAYED_TAG)) {
			DelayedTagsFile delayedTagsFile = new DelayedTagsFile();
			delayedTagsFile.writeUrlRevision(comp.getVcsRepository().getUrl(), headCommit.getRevision());
			progress.reportStatus("build commit " + headCommit.getRevision() + " is saved for delayed tagging");
		} else {
			TagDesc tagDesc = Utils.getTagDesc(versionToBuild.toString());
			Utils.reportDuration(() -> vcs.createTag(releaseBranchName, tagDesc.getName(), tagDesc.getMessage(), headCommit.getRevision()),
					String.format("tag head of %s: %s", releaseBranchName, tagDesc.getName()), null, progress);
		}
	}

	private Version raisePatchVersion(IProgress progress) {
		Version nextPatchVersion = versionToBuild.toNextPatch();
		Utils.reportDuration(() -> vcs.setFileContent(releaseBranchName, SCMReleaser.VER_FILE_NAME, nextPatchVersion.toString(),
				LogTag.SCM_VER + " " + nextPatchVersion),
				"bump patch version in release branch: " + nextPatchVersion, null, progress);
		return nextPatchVersion;
	}

}
