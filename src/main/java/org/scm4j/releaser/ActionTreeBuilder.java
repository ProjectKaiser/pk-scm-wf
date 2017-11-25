package org.scm4j.releaser;

import org.scm4j.commons.coords.Coords;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.scmactions.SCMActionRelease;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ActionTreeBuilder {

	public IAction getActionTree(Coords coords, ExtendedStatus extendedStatus) {
		List<IAction> childActions = new ArrayList<>();

		for (Map.Entry<Coords, ExtendedStatus> extendedStatusChild : extendedStatus.getSubComponents().entrySet()) {
			childActions.add(getActionTree(extendedStatusChild.getKey(), extendedStatusChild.getValue()));
		}
		return new SCMActionRelease(coords, childActions);
	}
}
