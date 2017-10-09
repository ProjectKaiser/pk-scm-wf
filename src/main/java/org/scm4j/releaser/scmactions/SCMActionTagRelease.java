package org.scm4j.releaser.scmactions;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.exceptions.EVCSTagExists;

import java.util.List;

public class SCMActionTagRelease extends ActionAbstract {

	private final ReleaseBranch rb;

	public SCMActionTagRelease(ReleaseBranch rb, List<IAction> childActions) {
		super(rb.getComponent(), childActions);
		this.rb = rb;
	}
	
	@Override
	public void execute(IProgress progress) {
		if (isUrlProcessed(comp.getVcsRepository().getUrl())) {
			progress.reportStatus("already executed");
			return;
		}
		try {
			super.executeChilds(progress);
			
			DelayedTagsFile cf = new DelayedTagsFile();
			IVCS vcs = getVCS();
			String revisionToTag = cf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
			if (revisionToTag == null) {
				progress.reportStatus("no revisions to dalayed tag");
				return;
			}
			
			Version delayedTagVersion = new Version(vcs.getFileContent(rb.getName(), SCMReleaser.VER_FILE_NAME, revisionToTag));
			TagDesc tagDesc = SCMReleaser.getTagDesc(delayedTagVersion.toString());

			try {
				vcs.createTag(rb.getName(), tagDesc.getName(), tagDesc.getMessage(), revisionToTag);
				progress.reportStatus(String.format("%s of %s tagged: %s", "commit " + revisionToTag, rb.getName(), delayedTagVersion.toReleaseString()));
			} catch (EVCSTagExists e) {
				progress.reportStatus(String.format("revision %s is already tagged with %s tag", revisionToTag, tagDesc.getName()));
			}
			
			cf.removeRevisionByUrl(comp.getVcsRepository().getUrl());

			addProcessedUrl(comp.getVcsRepository().getUrl());
		} catch (Exception e) {
			progress.error("execution error: " + e.toString());
			throw new EReleaserException(e);
		}
	}
	
	@Override
	public String toString() {
		return "tag " +  comp.getCoords().toString();
	}
}
