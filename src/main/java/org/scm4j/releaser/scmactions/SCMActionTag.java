package org.scm4j.releaser.scmactions;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.DelayedTagsFile;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.exceptions.EVCSTagExists;

import java.util.ArrayList;

public class SCMActionTag extends ActionAbstract {
	
	private final String releaseBranchName;

	public SCMActionTag(Component comp, String releaseBranchName, VCSRepository repo) {
		super(comp, new ArrayList<>(), repo);
		this.releaseBranchName = releaseBranchName;
	}
	
	@Override
	protected void executeAction(IProgress progress) {
		DelayedTagsFile dtf = new DelayedTagsFile();
		IVCS vcs = getVCS();
		String revisionToTag = dtf.getRevisitonByUrl(repo.getUrl());
		if (revisionToTag == null) {
			progress.reportStatus("no revisions to delayed tag");
			return;
		}

		Version delayedTagVersion = tagRevision(progress, vcs, revisionToTag);

		bumpPatch(progress, vcs, delayedTagVersion);

		dtf.removeRevisionByUrl(repo.getUrl());
	}

	private void bumpPatch(IProgress progress, IVCS vcs, Version delayedTagVersion) {
		Version nextPatchVersion = delayedTagVersion.toNextPatch();
		Version crbVersion = ReleaseBranchFactory.getCRB(repo).getVersion();
		if (!crbVersion.isGreaterThan(nextPatchVersion) && !crbVersion.equals(nextPatchVersion)) {
			Utils.reportDuration(() -> vcs.setFileContent(releaseBranchName, Utils.VER_FILE_NAME, nextPatchVersion.toString(),
					LogTag.SCM_VER + " " + nextPatchVersion),
					"bump patch version in release branch: " + nextPatchVersion, null, progress);
		}
	}

	private Version tagRevision(IProgress progress, IVCS vcs, String revisionToTag) {
		Version delayedTagVersion = new Version(vcs.getFileContent(null, Utils.VER_FILE_NAME, revisionToTag));
		TagDesc tagDesc = Utils.getTagDesc(delayedTagVersion.toString());
		try {
			Utils.reportDuration(() -> vcs.createTag(releaseBranchName, tagDesc.getName(), tagDesc.getMessage(), revisionToTag),
					String.format("tag revision %s of %s: %s", revisionToTag, releaseBranchName, delayedTagVersion.toReleaseString()), null, progress);
		} catch (EVCSTagExists e) {
			progress.reportStatus(String.format("revision %s is already tagged with %s tag", revisionToTag, tagDesc.getName()));
		}
		return delayedTagVersion;
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
