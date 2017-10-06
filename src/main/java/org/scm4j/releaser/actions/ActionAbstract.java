package org.scm4j.releaser.actions;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.conf.Component;
import org.scm4j.vcs.api.IVCS;

import java.util.ArrayList;
import java.util.List;

public abstract class ActionAbstract implements IAction {

	protected final List<IAction> childActions;
	protected final Component comp;
	protected final List<String> processedUrls = new ArrayList<>();
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

	protected boolean isUrlProcessed_(String url) {
		if(null == parent){
			return processedUrls.contains(url.toLowerCase());
		}
		return parent.isUrlProcessed(url);
		
	}
	
	public boolean isUrlProcessed(String url) {
		return isUrlProcessed_(url);
	}

	@Override
	public void setParent(IAction parent) {
		this.parent = parent;
	}

	@Override
	public void addProcessedUrl(String url) {
		if(null != parent){
			parent.addProcessedUrl(url);
		} else {
			processedUrls.add(url.toLowerCase());
		}
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
