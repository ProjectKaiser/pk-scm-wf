package org.scm4j.actions2;

import java.text.MessageFormat;

public class EActionFailed extends RuntimeException{
	private static final long serialVersionUID = 7403388570914785211L;
	public IAction2Executor executor;
	public IAction2 action;
	public Exception expection;

	public EActionFailed(IAction2Executor executor, IAction2 action, Exception expection){
		super(MessageFormat.format("FAILED: {0}/{1}: {2}", executor, action, expection));
		this.executor = executor;
		this.action = action;
		this.expection = expection;
	}
}
