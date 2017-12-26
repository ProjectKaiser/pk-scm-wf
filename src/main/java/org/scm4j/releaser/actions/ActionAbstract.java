package org.scm4j.releaser.actions;

import lombok.SneakyThrows;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.IVCS;

import java.util.ArrayList;
import java.util.List;

public abstract class ActionAbstract implements IAction {

	protected final List<IAction> childActions;
	protected final Component comp;
	protected final List<String> processedUrls = new ArrayList<>();
	protected IAction parent = null;
	protected final VCSRepository repo;

	public IVCS getVCS() {
		return repo.getVCS();
	}

	public ActionAbstract(Component comp, List<IAction> childActions, VCSRepository repo) {
		this.comp = comp;
		this.childActions = childActions;
		this.repo = repo;
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

	@SneakyThrows
	protected void executeChilds(IProgress progress) {
		for (IAction action : childActions) {
			try (IProgress nestedProgress = progress.createNestedProgress(action.toStringAction())) {
				action.execute(nestedProgress);
			}
		}
	}

	@Override
	public Component getComp() {
		return comp;
	}
	
	@Override
	public void execute(IProgress progress) {
		if (isUrlProcessed(repo.getUrl())) {
			progress.reportStatus("already executed");
			return;
		}
		
		executeChilds(progress);
		
		try {
			executeAction(progress);
			addProcessedUrl(repo.getUrl());
		} catch (Exception e) {
			progress.error("execution error: " + e.toString());
			if (!(e instanceof EReleaserException)) {
				throw new EReleaserException(e);
			}
			throw (EReleaserException) e;
		}
	}

	// throwing exception is used in tests only
	protected abstract void executeAction(IProgress progress) throws Exception;
}
