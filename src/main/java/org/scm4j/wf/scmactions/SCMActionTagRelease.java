package org.scm4j.wf.scmactions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.conf.CommitsFile;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.Option;
import org.scm4j.wf.conf.Version;

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
			String revisionToTag = cf.getRevisitonByUrl(comp.getVcsRepository().getUrl());
			if (revisionToTag == null) {
				progress.reportStatus("no revisions to dalayed tag");
				return;
			}
			
			List<VCSTag> tagsOnRevision = vcs.getTagsOnRevision(revisionToTag);
			ReleaseBranch rb = new ReleaseBranch(comp, repos);
			Version delayedTagVersion = new Version(vcs.getFileContent(rb.getName(), SCMWorkflow.VER_FILE_NAME, revisionToTag));
			for (VCSTag tag : tagsOnRevision) {
				if (tag.getTagName().equals(delayedTagVersion.toReleaseString())) {
					progress.reportStatus(String.format("revision %s is already tagged with %s tag", revisionToTag, tag.getTagName()));
					return;
				}
			}
			
			vcs.createTag(rb.getName(), delayedTagVersion.toReleaseString(), tagMessage, revisionToTag);
			
			cf.removeRevisionByUrl(comp.getVcsRepository().getUrl());
			progress.reportStatus(String.format("%s of %s tagged: %s", revisionToTag == null ? "head " : "commit " + revisionToTag, rb.getName(), delayedTagVersion.toReleaseString()));
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
