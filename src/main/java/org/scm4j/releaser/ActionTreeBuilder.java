package org.scm4j.releaser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.scmactions.SCMActionRelease;

public class ActionTreeBuilder {

	public IAction getActionTree(ExtendedStatusTreeNode node, CachedStatuses cache, ActionSet actionSet) {
		List<IAction> childActions = new ArrayList<>();
		for (Map.Entry<Component, ExtendedStatusTreeNode> nodeEntry : node.getSubComponents().entrySet()) {
			childActions.add(getActionTree(nodeEntry.getValue(), cache, actionSet));
		}
		
		return new SCMActionRelease(node.getComp(), childActions, cache, actionSet);
		
	}
}
