package org.scm4j.wf.scmactions;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.results.ActionResultTag;
import org.scm4j.wf.branch.DevelopBranch;
import org.scm4j.wf.conf.Component;

import java.util.List;

public class SCMActionTagRelease extends ActionAbstract {

	private final String tagMessage;

	public SCMActionTagRelease(Component dep, List<IAction> childActions, String tagMessage) {
		super(dep, childActions);
		this.tagMessage = tagMessage;
	}

	@Override
	public Object execute(IProgress progress) {
		try {
			
			ActionResultTag actionTag = (ActionResultTag) getResult(getName(), ActionResultTag.class);
			if (actionTag != null) {
				progress.reportStatus("already tagged: " + actionTag.toString());
				return actionTag;
			}
			
			Object nestedResult;
			for (IAction action : childActions) {
				try (IProgress nestedProgress = progress.createNestedProgress(action.toString())) {
					nestedResult = action.execute(nestedProgress);
					if (nestedResult instanceof Throwable) {
						return nestedResult;
					}
					addResult(action.getName(), nestedResult);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			
			IVCS vcs = getVCS();
			DevelopBranch db = new DevelopBranch(comp);
			
			String releaseBranchName = db.getPreviousMinorReleaseBranchName();
			String tagName = db.getVersion().toPreviousMinorRelease();
			VCSTag tag = vcs.createTag(releaseBranchName, tagName, tagMessage);
			progress.reportStatus("head of \"" + releaseBranchName + "\" tagged: " + tag.toString());
			return new ActionResultTag(getName(), tag);
		} catch (Throwable t) {
			progress.error(t.getMessage());
			return t;
		}
	}
	
	@Override
	public String toString() {
		return "tag " +  comp.getCoords().toString();
	}
}
