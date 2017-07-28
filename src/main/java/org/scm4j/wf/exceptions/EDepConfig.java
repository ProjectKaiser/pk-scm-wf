package org.scm4j.wf.exceptions;

public class EDepConfig extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public EDepConfig (Exception e) {
		super(e);
	}
	
	public EDepConfig(String message) {
		super(message);
	}

}
