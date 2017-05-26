package org.scm4j.actions2;

import org.scm4j.progress.IProgress;

public interface IAction2 {
	Object execute(IAction2Executor executor, IProgress progress) throws Exception;
	String getName();
}
