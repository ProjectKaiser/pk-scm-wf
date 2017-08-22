package org.scm4j.wf.actions;

import java.util.List;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.wf.conf.Component;

public abstract class ActionAbstract implements IAction {

	protected IAction parentAction;
	protected final List<IAction> childActions;
	protected final Component comp;

	public IVCS getVCS() {
		return comp.getVcsRepository().getVcs();
	}

	public ActionAbstract(Component comp, List<IAction> childActions) {
		this.comp = comp;
		this.childActions = childActions;
		
		if (childActions != null) {
			for (IAction action : childActions) {
				action.setParent(this);
			}
		}
	}

	public Component getComponent() {
		return comp;
	}

	@Override
	public IAction getParent() {
		return parentAction;
	}

	@Override
	public List<IAction> getChildActions() {
		return childActions;
	}

	@Override
	public void setParent(IAction parentAction) {
		this.parentAction = parentAction;
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
