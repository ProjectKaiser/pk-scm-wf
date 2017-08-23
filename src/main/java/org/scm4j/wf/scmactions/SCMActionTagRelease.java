package org.scm4j.wf.scmactions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.DevelopBranch;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.Option;
import org.yaml.snakeyaml.Yaml;

public class SCMActionTagRelease extends ActionAbstract {

	private final String tagMessage;

	public SCMActionTagRelease(Component dep, List<IAction> childActions, String tagMessage, List<Option> options) {
		super(dep, childActions, options);
		this.tagMessage = tagMessage;
	}

	@Override
	public void execute(IProgress progress) {
		try {
			/**
			 * add already tagged check
			 */
			
			for (IAction action : childActions) {
				try (IProgress nestedProgress = progress.createNestedProgress(action.toString())) {
					action.execute(nestedProgress);
				}
			}
			
			IVCS vcs = getVCS();
			DevelopBranch db = new DevelopBranch(comp);
			ReleaseBranch rb = new ReleaseBranch(comp, repos);
			String releaseBranchName = rb.getPreviousMinorReleaseBranchName();
			String tagName = db.getVersion().toPreviousMinor().toReleaseString();
			String revisionToTag = getRevisionToTag();
			vcs.createTag(releaseBranchName, tagName, tagMessage, revisionToTag);
			progress.reportStatus(String.format("%s of %s tagged: %s", revisionToTag == null ? "head of" : "commit " + revisionToTag, tagName));
		} catch (Throwable t) {
			progress.error(t.getMessage());
			throw new RuntimeException(t);
		}
	}
	
	private String getRevisionToTag() throws IOException {
		File commitsFile = new File(SCMWorkflow.BASE_WORKING_DIR, SCMWorkflow.COMMITS_FILE_NAME);
		if (!commitsFile.exists()) {
			return null;
		}
		
		Yaml yaml = new Yaml();
		@SuppressWarnings("unchecked")
		Map<String, Map<String, String>> repos = yaml.loadAs(FileUtils.readFileToString(commitsFile, StandardCharsets.UTF_8), Map.class);
		Map<String, String> comps = repos.get(comp.getVCS().getRepoUrl());
		if (comps == null) {
			return null;
		}
		
		return comps.get(comp.getName());
	}

	@Override
	public String toString() {
		return "tag " +  comp.getCoords().toString();
	}
}
