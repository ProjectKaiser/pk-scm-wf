package org.scm4j.wf.scmactions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.DevelopBranch;
import org.scm4j.wf.conf.Component;

public class SCMActionTagRelease extends ActionAbstract {

	private final String tagMessage;

	public SCMActionTagRelease(Component dep, List<IAction> childActions, String tagMessage) {
		super(dep, childActions);
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
			
			String releaseBranchName = db.getPreviousMinorReleaseBranchName();
			String tagName = db.getVersion().toPreviousMinor().toReleaseString();
			VCSTag tag = vcs.createTag(releaseBranchName, tagName, tagMessage);
			progress.reportStatus("head of \"" + releaseBranchName + "\" tagged: " + tag.toString());
		} catch (Throwable t) {
			progress.error(t.getMessage());
			throw new RuntimeException(t);
		}
	}
	
	@Override
	public String toString() {
		return "tag " +  comp.getCoords().toString();
	}
}
