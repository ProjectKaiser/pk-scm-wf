package org.scm4j.releaser.scmactions;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.CurrentReleaseBranch;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSTag;

import java.util.List;

public class SCMActionTagRelease extends ActionAbstract {

	private final CurrentReleaseBranch crb;

	public SCMActionTagRelease(CurrentReleaseBranch crb, List<IAction> childActions) {
		super(crb.getComponent(), childActions);
		this.crb = crb;
	}
	
	@Override
	public void execute(IProgress progress) {
		try {
			for (IAction action : childActions) {
				try (IProgress nestedProgress = progress.createNestedProgress(action.toString())) {
					action.execute(nestedProgress);
				}
			}
			
			DelayedTagsFile cf = new DelayedTagsFile();
			IVCS vcs = getVCS();
			String revisionToTag = cf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
			if (revisionToTag == null) {
				progress.reportStatus("no revisions to dalayed tag");
				return;
			}
			
			List<VCSTag> tagsOnRevision = vcs.getTagsOnRevision(revisionToTag);
			Version delayedTagVersion = new Version(vcs.getFileContent(crb.getName(), SCMReleaser.VER_FILE_NAME, revisionToTag));
			TagDesc tagDesc = SCMReleaser.getTagDesc(delayedTagVersion.toString());
			for (VCSTag tag : tagsOnRevision) {
				if (tag.getTagName().equals(tagDesc.getName())) {
					progress.reportStatus(String.format("revision %s is already tagged with %s tag", revisionToTag, tag.getTagName()));
					return;
				}
			}
			
			vcs.createTag(crb.getName(), tagDesc.getName(), tagDesc.getMessage(), revisionToTag);
			
			cf.removeRevisionByUrl(comp.getVcsRepository().getUrl());
			progress.reportStatus(String.format("%s of %s tagged: %s", "commit " + revisionToTag, crb.getName(), delayedTagVersion.toReleaseString()));
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
