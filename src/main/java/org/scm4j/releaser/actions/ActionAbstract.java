package org.scm4j.releaser.actions;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.conf.Component;
import org.scm4j.vcs.api.IVCS;

import java.util.ArrayList;
import java.util.List;

public abstract class ActionAbstract implements IAction {

	protected final List<IAction> childActions;
	protected final Component comp;
	protected final List<Component> processedComps = new ArrayList<>();
	protected IAction parent = null;

	public IVCS getVCS() {
		return comp.getVCS();
	}

	public ActionAbstract(Component comp, List<IAction> childActions) {
		this.comp = comp;
		this.childActions = childActions;
		for (IAction childAction : childActions) {
			childAction.setParent(this);
		}
	}

	protected boolean isCompProcessed_(Component comp) {
		if(null == parent){
			return processedComps.contains(comp);
		}
		return parent.isCompProcessed(comp);
		
	}
	
	public boolean isCompProcessed(Component comp) {
		return isCompProcessed_(comp);
	}

	@Override
	public void setParent(IAction parent) {
		this.parent = parent;
	}

	@Override
	public IAction getParent() {
		if (parent != null) {
			return parent.getParent();
		}
		return this;
	}

	@Override
	public void addProcessedComp(Component comp) {
		if(null != parent){
			parent.addProcessedComp(comp);
		} else {
			processedComps.add(comp);
		}
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

	protected void executeChilds(IProgress progress) throws Exception {
		for (IAction action : childActions) {
			try (IProgress nestedProgress = progress.createNestedProgress(action.toString())) {
				action.execute(nestedProgress);
			}
		}
	}

}
