package org.scm4j.releaser.scmactions;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.*;
import org.scm4j.releaser.exceptions.ENoBuilder;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

	
public class SCMActionBuild extends ActionAbstract {

	private final IVCS vcs;
	private final ReleaseBranch rb;
	private final BuildStatus mbs;

	public SCMActionBuild(ReleaseBranch rb, List<IAction> childActions, BuildStatus mbs) {
		super(rb.getComponent(), childActions);
		this.rb = rb;
		vcs = getVCS();
		this.mbs = mbs;
	}

	@Override
	public void execute(IProgress progress) {
		if (isUrlProcessed(comp.getVcsRepository().getUrl())) {
			progress.reportStatus("already executed");
			return;
		}
		try {
			super.executeChilds(progress);

			progress.reportStatus("target version: " + rb.getVersion());
			switch(mbs) {
			case BUILD_MDEPS:
			case ACTUALIZE_PATCHES:
				actualizePatches(progress);
			case BUILD:
				if (comp.getVcsRepository().getBuilder() == null) {
					throw new ENoBuilder(comp);
				}
				VCSCommit headCommit = vcs.getHeadCommit(rb.getName());
				build(progress, headCommit);
				tagBuild(progress, headCommit);
				raisePatchVersion(progress);
				progress.reportStatus(comp.getName() + " " + rb.getVersion().toString() + " is built in " + rb.getName());
				break;
			default:
				throw new IllegalStateException(mbs + " target action is occured when build only is expected");
			}
			addProcessedUrl(comp.getVcsRepository().getUrl());
		} catch (Exception e) {
			progress.error("execution error: " + e.toString());
			if (!(e instanceof EReleaserException)) {
				throw new EReleaserException(e);
			}
			throw (EReleaserException) e;
		}
	}

	protected void actualizePatches(IProgress progress) throws Exception {
		MDepsFile currentMDepsFile = rb.getMDepsFile();
		StringBuilder sb = new StringBuilder();
		boolean hasNew = false;
		ReleaseBranch rbMDep;
		Version newVersion;
		for (Component currentMDep : currentMDepsFile.getMDeps()) {
			rbMDep = new ReleaseBranch(currentMDep);
			newVersion = rbMDep.getVersion().toPreviousPatch(); // TODO: which patch of mdep to actualize on if there are no mdep release branches at all?
			if (!newVersion.equals(currentMDep.getVersion())) {
				hasNew = true;
			}
			
			if (hasNew) {
				sb.append("" + currentMDep.getName() + ": " + currentMDep.getVersion() + " -> " + newVersion + "\r\n");
			}
			currentMDepsFile.replaceMDep(currentMDep.clone(newVersion));
		}
		if (hasNew) {
			vcs.setFileContent(rb.getName(), SCMReleaser.MDEPS_FILE_NAME, currentMDepsFile.toFileContent(), LogTag.SCM_MDEPS);
			progress.reportStatus("mdeps actualized" + (sb.length() == 0 ? "" : ":\r\n" + StringUtils.removeEnd(sb.toString(), "\r\n")));
		} else {
			progress.reportStatus("mdeps are actual already");
		}
	}

	private void build(IProgress progress, VCSCommit headCommit) throws Exception {
		File buildDir = rb.getBuildDir();
		if (buildDir.exists()) {
			FileUtils.deleteDirectory(buildDir);
		}
		Files.createDirectories(buildDir.toPath());		
		progress.reportStatus(String.format("checking out %s on revision %s into %s", getName(), headCommit.getRevision(), buildDir.getPath()));
		vcs.checkout(rb.getName(), buildDir.getPath(), headCommit.getRevision());
		comp.getVcsRepository().getBuilder().build(comp, buildDir, progress);
	}

	private void tagBuild(IProgress progress, VCSCommit headCommit) throws IOException {
		if (Options.hasOption(Option.DELAYED_TAG)) {
			DelayedTagsFile delayedTagsFile = new DelayedTagsFile();
			delayedTagsFile.writeUrlRevision(comp.getVcsRepository().getUrl(), headCommit.getRevision());
			progress.reportStatus("build commit " + headCommit.getRevision() + " is saved for delayed tagging");
		} else {
			String releaseBranchName = rb.getName();
			TagDesc tagDesc = SCMReleaser.getTagDesc(rb.getVersion().toString());
			VCSTag tag = vcs.createTag(releaseBranchName, tagDesc.getName(), tagDesc.getMessage(), headCommit.getRevision());
			progress.reportStatus("head of \"" + releaseBranchName + "\" tagged: " + tag.toString());
		}
	}

	private void raisePatchVersion(IProgress progress) {
		Version nextPatchVersion = rb.getVersion().toNextPatch();
		vcs.setFileContent(rb.getName(), SCMReleaser.VER_FILE_NAME, nextPatchVersion.toString(),
				LogTag.SCM_VER + " " + nextPatchVersion);
		progress.reportStatus("patch version is bumped in release branch: " + nextPatchVersion);
	}

	@Override
	public String toString() {
		return mbs + " " + comp.getCoords().toString() + ", version: " + rb.getVersion().toString();
	}

	public Version getVersion() {
		return rb.getVersion();
	}
	
	public ReleaseBranch getReleaseBranch() {
		return rb;
	}

	public BuildStatus getMbs() {
		return mbs;
	}
}
