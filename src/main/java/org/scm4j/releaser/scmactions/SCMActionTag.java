package org.scm4j.releaser.scmactions;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.conf.*;
import org.scm4j.releaser.exceptions.ENoDelayedTags;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.exceptions.EVCSTagExists;

import java.util.ArrayList;

public class SCMActionTag extends ActionAbstract {
	
	public SCMActionTag(Component comp, VCSRepository repo) {
		super(comp, new ArrayList<>(), repo);
	}
	
	@Override
	protected void executeAction(IProgress progress) {
		IVCS vcs = getVCS();
		DelayedTagsFile dtf = new DelayedTagsFile();
		DelayedTag delayedTag = dtf.getDelayedTagByUrl(repo.getUrl());
		if (delayedTag == null) {
			throw new ENoDelayedTags(repo.getUrl());
		}

		String branchName = Utils.getReleaseBranchName(repo, delayedTag.getVersion());

		tagRevision(progress, vcs, delayedTag, branchName);

		bumpPatch(progress, vcs, delayedTag, branchName);

		new DelayedTagsFile().removeTagByUrl(repo.getUrl());
	}

	private void bumpPatch(IProgress progress, IVCS vcs, DelayedTag delayedTag, String branchName) {
		Version nextPatchVersion = delayedTag.getVersion().toNextPatch();
		Version crbVersion = ReleaseBranchFactory.getCRB(repo).getVersion();
		if (!crbVersion.isGreaterThan(nextPatchVersion) && !crbVersion.equals(nextPatchVersion)) {
			Utils.reportDuration(() -> vcs.setFileContent(branchName, Utils.VER_FILE_NAME, nextPatchVersion.toString(),
					LogTag.SCM_VER + " " + nextPatchVersion),
					String.format("bump patch version in release branch %s: %s", branchName, nextPatchVersion), null, progress);
		}
	}

	private void tagRevision(IProgress progress, IVCS vcs, DelayedTag delayedTag, String branchName) {
		TagDesc tagDesc = Utils.getTagDesc(delayedTag.getVersion().toString());
		try {
			Utils.reportDuration(() -> vcs.createTag(branchName, tagDesc.getName(), tagDesc.getMessage(), delayedTag.getRevision()),
					String.format("tag revision %s of %s: %s", delayedTag.getRevision(), branchName,
							delayedTag.getVersion()), null, progress);
		} catch (EVCSTagExists e) {
			progress.reportStatus(String.format("tag %s already exists", tagDesc.getName()));
		}
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
