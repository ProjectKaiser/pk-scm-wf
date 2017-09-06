package org.scm4j.wf.actions;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.Option;

import java.util.List;

public abstract class ActionAbstract implements IAction {

	protected final List<IAction> childActions;
	protected final Component comp;
	protected final List<Option> options;

	public IVCS getVCS() {
		return comp.getVCS();
	}

	public ActionAbstract(Component comp, List<IAction> childActions, List<Option> options) {
		this.comp = comp;
		this.childActions = childActions;
		this.options = options;
	}

	public Component getComponent() {
		return comp;
	}

	@Override
	public List<IAction> getChildActions() {
		return childActions;
	}

	@Override
	public String getName() {
		return comp.getName();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [" + comp.getName() + "]";
	}
}
