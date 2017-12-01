package org.scm4j.releaser;

import org.scm4j.commons.Version;
import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.WorkingBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.scmactions.SCMActionTag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SCMReleaser {

	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String DELAYED_TAGS_FILE_NAME = "delayed-tags.yml";
	public static final File BASE_WORKING_DIR = new File(System.getProperty("user.home"), ".scm4j");

	public IAction getActionTree(Component comp) {
		return getActionTree(comp, ActionSet.FULL);
	}

	public IAction getActionTree(String coords) {
		return getActionTree(new Component(coords));
	}

	public IAction getActionTree(String coords, ActionSet actionSet) {
		return getActionTree(new Component(coords), actionSet);
	}

	public IAction getActionTree(Component comp, ActionSet actionSet) {
		CachedStatuses cache = new CachedStatuses();
		ExtendedStatusTreeBuilder statusBuilder = new ExtendedStatusTreeBuilder();
		ExtendedStatusTreeNode node = statusBuilder.getExtendedStatusTreeNode(comp, cache);

		ActionTreeBuilder actionBuilder = new ActionTreeBuilder();
		return actionBuilder.getActionTree(node, cache, actionSet);
	}

	public IAction getTagActionTree(Component comp) {
		List<IAction> childActions = new ArrayList<>();
		List<Component> mDeps = WorkingBranch.getMDepsDevelop(comp);

		for (Component mDep : mDeps) {
			childActions.add(getTagActionTree(mDep));
		}

		Version lastReleaseVersion = db.getVersion().toPreviousMinor();

		return new SCMActionTag(comp, childActions, Utils.getReleaseBranchName(comp, lastReleaseVersion));
	}


}
