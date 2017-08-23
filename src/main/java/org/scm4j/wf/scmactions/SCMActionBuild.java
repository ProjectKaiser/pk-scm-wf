package org.scm4j.wf.scmactions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
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
import org.yaml.snakeyaml.Yaml;

	
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
			if (rbs == ReleaseBranchStatus.BUILT || rbs == ReleaseBranchStatus.TAGGED) {
				progress.reportStatus("version " + rb.getTargetVersion().toString() + " already built ");
				return;
				//return new ActionResultVersion(comp.getName(), rb.getTargetVersion().toString(), true, rb.getReleaseBranchName());
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
			
			// need to detect that we are built already
			VCSCommit builtCommit = vcs.setFileContent(rb.getReleaseBranchName(), SCMWorkflow.VER_FILE_NAME, targetVersion.toString(), LogTag.SCM_BUILT + " " + targetVersion.toString());
			if (builtCommit.equals(VCSCommit.EMPTY)) {
				throw new RuntimeException("built tag can not be set because release version is not changed");
			}
			if (options.contains(Option.DELAYED_TAG)) {
				saveDelayedTag(headCommit);
				progress.reportStatus("build commit " + headCommit.getRevision() + " is saved for delayed tagging");
			} else {
				
				
				String releaseBranchName = rb.getPreviousMinorReleaseBranchName();
				String tagName = rb.getTargetVersion().toString();
				String tagMessage = tagName + " release"; 
				VCSTag tag = vcs.createTag(releaseBranchName, tagName, tagMessage, headCommit.getRevision());
				progress.reportStatus("head of \"" + releaseBranchName + "\" tagged: " + tag.toString());
			}
			
			progress.reportStatus(comp.getName() + " " + targetVersion.toString() + " is built in " + rb.getReleaseBranchName());
		} catch (Throwable t) {
			progress.error("execution error: " + t.toString() + ": " + t.getMessage());
			throw new RuntimeException(t);
		} 
	}

	private void saveDelayedTag(VCSCommit commitToTag) throws IOException {
		File commitsFile = SCMWorkflow.getCommitsFile();
		if (!commitsFile.exists()) {
			commitsFile.createNewFile();
		}
		
		Yaml yaml = new Yaml();
		@SuppressWarnings("unchecked")
		Map<String, Map<String, String>> repos = yaml.loadAs(FileUtils.readFileToString(commitsFile, StandardCharsets.UTF_8), Map.class);
		if (repos == null) {
			repos = new HashMap<>();
		}
		Map<String, String> comps = repos.get(comp.getVCS().getRepoUrl());
		if (comps == null) {
			comps = new HashMap<>();
			repos.put(comp.getVCS().getRepoUrl(), comps);
		}
		comps.put(comp.getName(), commitToTag.getRevision());
		
		FileUtils.writeStringToFile(commitsFile, yaml.dumpAsMap(repos), StandardCharsets.UTF_8);
	}

	@Override
	public String toString() {
		return "build " + comp.getCoords().toString() + ", targetVersion=" + targetVersion.toString() + ", " + reason.toString();
	}
}
