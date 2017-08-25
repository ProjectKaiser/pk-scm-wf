package org.scm4j.wf.scmactions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.DevelopBranch;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.Option;

public class SCMActionTagRelease extends ActionAbstract {

	private final String tagMessage;

	public SCMActionTagRelease(Component dep, List<IAction> childActions, String tagMessage, List<Option> options) {
		super(dep, childActions, options);
		this.tagMessage = tagMessage;
	}
	
	@Override
	public void execute(IProgress progress) {
		try {
			for (IAction action : childActions) {
				try (IProgress nestedProgress = progress.createNestedProgress(action.toString())) {
					action.execute(nestedProgress);
				}
			}
			
			CommitsFile cf = new CommitsFile();
			IVCS vcs = getVCS();
			String revisionToTag = cf.getRevisitonByComp(comp.getName());
			DevelopBranch db = new DevelopBranch(comp);
			ReleaseBranch rb = db.getCurrentReleaseBranch(repos);
			if (revisionToTag == null) {
				revisionToTag = vcs.getHeadCommit(rb.getReleaseBranchName()).getRevision();
			}
			
			if (vcs.isRevisionTagged(revisionToTag)) {
				progress.reportStatus(String.format("revision %s is already tagged", revisionToTag));
				return;
			}
			
			String releaseBranchName = rb.getReleaseBranchName();
			String tagName = rb.getVersion().toReleaseString();
			vcs.createTag(releaseBranchName, tagName, tagMessage, revisionToTag);
			progress.reportStatus(String.format("%s of %s tagged: %s", revisionToTag == null ? "head " : "commit " + revisionToTag, rb.getReleaseBranchName(), tagName));
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
