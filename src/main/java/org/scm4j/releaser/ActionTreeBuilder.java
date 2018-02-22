package org.scm4j.releaser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.conf.VCSRepositoryFactory;
import org.scm4j.releaser.exceptions.EBuildOnNotForkedRelease;
import org.scm4j.releaser.exceptions.EInconsistentCompState;
import org.scm4j.releaser.scmactions.SCMActionRelease;
import org.scm4j.releaser.scmactions.SCMActionTag;

public class ActionTreeBuilder {
	
	private final VCSRepositoryFactory repoFactory;

	public ActionTreeBuilder(VCSRepositoryFactory repoFactory) {
		this.repoFactory = repoFactory;
	}

	public IAction getTagAction(String coords) {
		return getTagAction(new Component(coords));
	}
	
	public IAction getActionTreeDelayedTag(ExtendedStatus node, CachedStatuses cache) {
		return getActionTree(node, cache, ActionSet.FULL, true);
	}
	
	public IAction getActionTreeFull(ExtendedStatus node, CachedStatuses cache) {
		return getActionTree(node, cache, ActionSet.FULL, false);
	}
	
	public IAction getActionTreeForkOnly(ExtendedStatus node, CachedStatuses cache) {
		return getActionTree(node, cache, ActionSet.FORK_ONLY, false);
	}
	
	private IAction getActionTree(ExtendedStatus node, CachedStatuses cache, ActionSet actionSet, boolean delayedTag) {
		List<IAction> childActions = new ArrayList<>();
		for (Map.Entry<Component, ExtendedStatus> nodeEntry : node.getSubComponents().entrySet()) {
			childActions.add(getActionTree(nodeEntry.getValue(), cache, actionSet, false));
		}
		
		if (node.getStatus() == BuildStatus.ERROR) {
			throw new EInconsistentCompState(node.getComp(), node.getErrorDesc());
		}

		if (node.getStatus() == BuildStatus.FORK && actionSet == ActionSet.FULL) {
			throw new EBuildOnNotForkedRelease(node.getComp());
		}

		VCSRepository repo = repoFactory.getVCSRepository(node.getComp());
		return new SCMActionRelease(node.getComp(), childActions, cache, repoFactory, actionSet, delayedTag, repo);
	}

	public IAction getTagAction(Component comp) {
		VCSRepository repo = repoFactory.getVCSRepository(comp);
//		DelayedTagsFile dtf = new DelayedTagsFile();
//		String revisionToTag = dtf.getRevisitonByUrl(repo.getUrl());
//		if (revisionToTag == null) {
//			throw new ENoRevisionsToTag();
//		}
		// FIXME: fix
		Version lastReleaseVersion = new DevelopBranch(comp, repo).getVersion().toPreviousMinor();
				//new Version(repo.getVCS().getFileContent(null, Utils.VER_FILE_NAME, dtf.getRevisitonByUrl(repo.getUrl())));
		return new SCMActionTag(comp, Utils.getReleaseBranchName(repo, lastReleaseVersion), repo);
	}
}
