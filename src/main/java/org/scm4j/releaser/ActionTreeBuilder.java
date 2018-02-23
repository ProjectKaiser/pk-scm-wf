package org.scm4j.releaser;

import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.*;
import org.scm4j.releaser.exceptions.EBuildOnNotForkedRelease;
import org.scm4j.releaser.exceptions.EDelayingDelayed;
import org.scm4j.releaser.exceptions.ENoDelayedTags;
import org.scm4j.releaser.scmactions.SCMActionRelease;
import org.scm4j.releaser.scmactions.SCMActionTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
		VCSRepository repo = repoFactory.getVCSRepository(node.getComp());

		if (delayedTag && alreadyDelayed(repo.getUrl())) {
			throw new EDelayingDelayed(repo.getUrl());
		}

		for (Map.Entry<Component, ExtendedStatus> nodeEntry : node.getSubComponents().entrySet()) {
			childActions.add(getActionTree(nodeEntry.getValue(), cache, actionSet, false));
		}

		if (node.getStatus() == BuildStatus.FORK && actionSet == ActionSet.FULL) {
			throw new EBuildOnNotForkedRelease(node.getComp());
		}

		return new SCMActionRelease(node.getComp(), childActions, cache, repoFactory, actionSet, delayedTag, repo);
	}

	private boolean alreadyDelayed(String url) {
		DelayedTagsFile dtf = new DelayedTagsFile();
		return dtf.getDelayedTagByUrl(url) != null;
	}

	public IAction getTagAction(Component comp) {
		VCSRepository repo = repoFactory.getVCSRepository(comp);
		DelayedTagsFile dtf = new DelayedTagsFile();
		DelayedTag delayedTag = dtf.getDelayedTagByUrl(repo.getUrl());
		if (delayedTag == null) {
			throw new ENoDelayedTags(repo.getUrl());
		}
		return new SCMActionTag(comp, repo);
	}
}
