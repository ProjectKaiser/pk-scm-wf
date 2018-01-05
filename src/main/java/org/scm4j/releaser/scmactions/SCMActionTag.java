package org.scm4j.releaser.scmactions;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.*;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.exceptions.EVCSTagExists;

import java.util.List;

public class SCMActionTag extends ActionAbstract {
	
	private final String releaseBranchName;

	public SCMActionTag(Component comp, List<IAction> childActions, String releaseBranchName, VCSRepository repo) {
		super(comp, childActions, repo);
		this.releaseBranchName = releaseBranchName;
	}
	
	@Override
	protected void executeAction(IProgress progress) {
		DelayedTagsFile cf = new DelayedTagsFile();
		IVCS vcs = getVCS();
		String revisionToTag = cf.getRevisitonByUrl(repo.getUrl());
		if (revisionToTag == null) {
			progress.reportStatus("no revisions to delayed tag");
			return;
		}
		
		Version delayedTagVersion = new Version(vcs.getFileContent(releaseBranchName, Utils.VER_FILE_NAME, revisionToTag));
		TagDesc tagDesc = Utils.getTagDesc(delayedTagVersion.toString());

		try {
			Utils.reportDuration(() -> vcs.createTag(releaseBranchName, tagDesc.getName(), tagDesc.getMessage(), revisionToTag),
					String.format("tag revision %s of %s: %s", revisionToTag, releaseBranchName, delayedTagVersion.toReleaseString()), null, progress);
		} catch (EVCSTagExists e) {
			progress.reportStatus(String.format("revision %s is already tagged with %s tag", revisionToTag, tagDesc.getName()));
		}
		
		cf.removeRevisionByUrl(repo.getUrl());
	}
	
	@Override
	public String toString() {
		return "tag " +  comp.getCoords().toString();
	}

	@Override
	public String toStringAction() {
		return toString();
	}

	@Override
	public boolean isExecutable() {
		return true;
	}
}
