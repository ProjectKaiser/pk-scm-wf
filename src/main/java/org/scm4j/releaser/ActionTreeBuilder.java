package org.scm4j.releaser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.MDepsSource;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.scmactions.SCMActionRelease;
import org.scm4j.releaser.scmactions.SCMActionTag;

public class ActionTreeBuilder {

	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String DELAYED_TAGS_FILE_NAME = "delayed-tags.yml";
	public static final File BASE_WORKING_DIR = new File(System.getProperty("user.home"), ".scm4j");
	
	public IAction getActionTree(Component comp) {
		return getActionTreeFull(comp, false);
	}

	public IAction getActionTree(String coords) {
		return getActionTreeFull(coords, false);
	}

	public IAction getTagActionTree(String coords) {
		return getTagActionTree(new Component(coords));
	}
	
	public IAction getActionTreeForkOnly(String coords) {
		return getActionTree(new Component(coords), ActionSet.FORK_ONLY, false, false);
	}
	
	public IAction getActionTreeForkOnly(Component comp) {
		return getActionTree(comp, ActionSet.FORK_ONLY, false, false);
	}
	
	public IAction getActionTreeDelayedTag(String coords) {
		return getActionTreeFull(coords, true);
	}
	
	public IAction getActionTreeDelayedTag(Component comp) {
		return getActionTreeFull(comp, true);
	}
	
	private IAction getActionTreeFull(String coords, boolean delayedTag) {
		return getActionTreeFull(new Component(coords), delayedTag);
	}
	
	private IAction getActionTreeFull(Component comp, boolean delayedTag) {
		return getActionTree(comp, ActionSet.FULL, delayedTag, !comp.getVersion().isEmpty());
	}
	
	private IAction getActionTree(Component comp, ActionSet actionSet, boolean delayedTag, boolean isForPatch) {
		CachedStatuses cache = new CachedStatuses();
		ExtendedStatusTreeBuilder statusBuilder = new ExtendedStatusTreeBuilder();
		ExtendedStatusTreeNode node = isForPatch ? 
				statusBuilder.getExtendedStatusTreeNodeForPatch(comp, cache) :
				statusBuilder.getExtendedStatusTreeNode(comp, cache);

		return getActionTree(node, cache, actionSet, delayedTag);
	}
	
	public IAction getActionTree(ExtendedStatusTreeNode node, CachedStatuses cache, ActionSet actionSet, boolean delayedTag) {
		List<IAction> childActions = new ArrayList<>();
		for (Map.Entry<Component, ExtendedStatusTreeNode> nodeEntry : node.getSubComponents().entrySet()) {
			childActions.add(getActionTree(nodeEntry.getValue(), cache, actionSet, delayedTag));
		}
		
		return new SCMActionRelease(node.getComp(), childActions, cache, actionSet, delayedTag);
	}

	public IAction getTagActionTree(Component comp) {
		List<IAction> childActions = new ArrayList<>();
		List<Component> mDeps = MDepsSource.getMDepsDevelop(comp);

		for (Component mDep : mDeps) {
			childActions.add(getTagActionTree(mDep));
		}

		DevelopBranch db = new DevelopBranch(comp);
		Version lastReleaseVersion = db.getVersion().toPreviousMinor();

		return new SCMActionTag(comp, childActions, Utils.getReleaseBranchName(comp, lastReleaseVersion));
	}
	
}
