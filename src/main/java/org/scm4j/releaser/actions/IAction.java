package org.scm4j.releaser.actions;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.conf.Component;

import java.util.List;

public interface IAction {

	void execute(IProgress progress);

	List<IAction> getChildActions();

	String getName();

	void setParent(IAction parent);

	void addProcessedUrl(String url);

	boolean isUrlProcessed(String url);

	Component getComp();

	String toStringAction();
}
