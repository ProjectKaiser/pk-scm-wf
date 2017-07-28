package org.scm4j.wf.exceptions;

public class EBuilder extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public EBuilder(Exception e) {
		super(e);
	}

	public EBuilder(String message) {
		super(message);
	}

}
