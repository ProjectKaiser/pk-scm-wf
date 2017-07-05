package org.scm4j.wf.exceptions;

public class EConfig extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public EConfig(String message) {
		super(message);
	}
	
	public EConfig(String message, Throwable t) {
		super(message, t);
	}

	public EConfig(Exception e) {
		super(e);
	}

	
}
