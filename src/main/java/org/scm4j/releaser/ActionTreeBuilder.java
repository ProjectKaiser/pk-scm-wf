package org.scm4j.releaser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.exceptions.EBuildOnNotForkedRelease;
import org.scm4j.releaser.scmactions.SCMActionRelease;
import org.scm4j.releaser.scmactions.SCMActionTag;

public class ActionTreeBuilder {

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
		
		if (node.getStatus() == BuildStatus.FORK && actionSet == ActionSet.FULL) {
			throw new EBuildOnNotForkedRelease(node.getComp());
		}
		
		return new SCMActionRelease(node.getComp(), childActions, cache, actionSet, delayedTag);
	}

	public IAction getTagAction(Component comp) {
		List<IAction> childActions = new ArrayList<>();
		Version lastReleaseVersion = new DevelopBranch(comp).getVersion().toPreviousMinor();
		return new SCMActionTag(comp, childActions, Utils.getReleaseBranchName(comp, lastReleaseVersion));
	}
}
