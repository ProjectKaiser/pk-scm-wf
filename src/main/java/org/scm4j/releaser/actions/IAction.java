package org.scm4j.releaser.actions;

import java.util.List;

import org.scm4j.commons.progress.IProgress;

public interface IAction {

	void execute(IProgress progress);

	List<IAction> getChildActions();

	String getName();

	void setParent(IAction parent);

	void addProcessedUrl(String url);

	boolean isUrlProcessed(String url);
}
