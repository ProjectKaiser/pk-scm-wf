package org.scm4j.releaser.scmactions;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.exceptions.EVCSTagExists;

import java.util.List;

public class SCMActionTag extends ActionAbstract {

	private final ReleaseBranch rb;

	public SCMActionTag(ReleaseBranch rb, Component comp, List<IAction> childActions) {
		super(comp, childActions);
		this.rb = rb;
	}
	
	@Override
	protected void executeAction(IProgress progress) {
		DelayedTagsFile cf = new DelayedTagsFile();
		IVCS vcs = getVCS();
		String revisionToTag = cf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
		if (revisionToTag == null) {
			progress.reportStatus("no revisions to delayed tag");
			return;
		}
		
		Version delayedTagVersion = new Version(vcs.getFileContent(rb.getName(), SCMReleaser.VER_FILE_NAME, revisionToTag));
		TagDesc tagDesc = SCMReleaser.getTagDesc(delayedTagVersion.toString());

		try {
			SCMReleaser.reportDuration(() -> vcs.createTag(rb.getName(), tagDesc.getName(), tagDesc.getMessage(), revisionToTag),
					String.format("tag revision %s of %s: %s", revisionToTag, rb.getName(), delayedTagVersion.toReleaseString()), null, progress);
		} catch (EVCSTagExists e) {
			progress.reportStatus(String.format("revision %s is already tagged with %s tag", revisionToTag, tagDesc.getName()));
		}
		
		cf.removeRevisionByUrl(comp.getVcsRepository().getUrl());
	}
	
	@Override
	public String toString() {
		return "tag " +  comp.getCoords().toString();
	}

	@Override
	public String toStringAction() {
		return toString();
	}
}
