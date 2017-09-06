package org.scm4j.wf.scmactions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.ActionAbstract;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branch.ReleaseBranch;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.DelayedTagsFile;
import org.scm4j.wf.conf.Option;
import org.scm4j.wf.conf.TagDesc;
import org.scm4j.wf.conf.Version;

public class SCMActionTagRelease extends ActionAbstract {

	public SCMActionTagRelease(Component dep, List<IAction> childActions, List<Option> options) {
		super(dep, childActions, options);
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
			ReleaseBranch rb = new ReleaseBranch(comp, repos);
			Version delayedTagVersion = new Version(vcs.getFileContent(rb.getName(), SCMWorkflow.VER_FILE_NAME, revisionToTag));
			TagDesc tagDesc = SCMWorkflow.getTagDesc(delayedTagVersion.toString());
			for (VCSTag tag : tagsOnRevision) {
				if (tag.getTagName().equals(tagDesc.getName())) {
					progress.reportStatus(String.format("revision %s is already tagged with %s tag", revisionToTag, tag.getTagName()));
					return;
				}
			}
			
			vcs.createTag(rb.getName(), tagDesc.getName(), tagDesc.getMessage(), revisionToTag);
			
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
