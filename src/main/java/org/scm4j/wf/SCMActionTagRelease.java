package org.scm4j.wf;

import java.util.List;

import org.scm4j.actions.ActionAbstract;
import org.scm4j.actions.IAction;
import org.scm4j.actions.results.ActionResultTag;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.conf.VCSRepository;

public class SCMActionTagRelease extends ActionAbstract {

	private String tagMessage;

	public SCMActionTagRelease(VCSRepository repo, List<IAction> childActions, String currentBranchName,
			IVCSWorkspace ws, String tagMessage) {
		super(repo, childActions, currentBranchName, ws);
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
				try (IProgress nestedProgress = progress.createNestedProgress(action.getName())) {
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
			
			String releaseBranchName = repo.getReleaseBanchPrefix() + getDevVersion().toPreviousMinorRelease();
			String tagName = getDevVersion().toPreviousMinorRelease();
			VCSTag tag = vcs.createTag(releaseBranchName, tagName, tagMessage);
			progress.reportStatus("head of \"" + releaseBranchName + "\" tagged: " + tag.toString());
			return new ActionResultTag(getName(), tag);
		} catch (Throwable t) {
			progress.error(t.getMessage());
			return t;
		}
	}
}
