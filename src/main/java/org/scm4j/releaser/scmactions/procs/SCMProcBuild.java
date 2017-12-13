package org.scm4j.releaser.scmactions.procs;

import lombok.SneakyThrows;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.*;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.exceptions.ENoBuilder;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;

import java.io.File;
import java.nio.file.Files;

public class SCMProcBuild implements ISCMProc {
	
	private final IVCS vcs;
	private final Component comp;
	private final String releaseBranchName;
	private final Version versionToBuild;
	private final CachedStatuses cache;
	private final boolean delayedTag;
 
	public SCMProcBuild(Component comp, CachedStatuses cache, boolean delayedTag) {
		this.comp = comp;
		vcs = comp.getVCS();
		this.cache = cache;
		releaseBranchName = Utils.getReleaseBranchName(comp, cache.get(comp.getUrl()).getNextVersion());
		versionToBuild = cache.get(comp.getUrl()).getNextVersion();
		this.delayedTag = delayedTag;
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
		
		ExtendedStatus existing = cache.get(comp.getUrl());
		cache.replace(comp.getUrl(), new ExtendedStatus(versionToBuild.toNextPatch(), existing.getStatus(), existing.getSubComponents(), comp));
		
		progress.reportStatus(comp.getName() + " " + versionToBuild + " is built in " + releaseBranchName);
	}
	
	@SneakyThrows
	private void build(IProgress progress, VCSCommit headCommit) {
		File buildDir = Utils.getBuildDir(comp, versionToBuild);
		if (buildDir.exists()) {
			Utils.waitForDeleteDir(buildDir);
		}
		Files.createDirectories(buildDir.toPath());		
		Utils.reportDuration(() -> vcs.checkout(releaseBranchName, buildDir.getPath(), headCommit.getRevision()),
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
			TagDesc tagDesc = Utils.getTagDesc(versionToBuild.toString());
			Utils.reportDuration(() -> vcs.createTag(releaseBranchName, tagDesc.getName(), tagDesc.getMessage(), headCommit.getRevision()),
					String.format("tag head of %s: %s", releaseBranchName, tagDesc.getName()), null, progress);
		}
	}

	private void raisePatchVersion(IProgress progress) {
		Version nextPatchVersion = versionToBuild.toNextPatch();
		Utils.reportDuration(() -> vcs.setFileContent(releaseBranchName, ActionTreeBuilder.VER_FILE_NAME, nextPatchVersion.toString(),
				LogTag.SCM_VER + " " + nextPatchVersion),
				"bump patch version in release branch: " + nextPatchVersion, null, progress);
	}
}
