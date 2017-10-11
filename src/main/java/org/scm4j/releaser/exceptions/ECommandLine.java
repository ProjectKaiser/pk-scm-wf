package org.scm4j.releaser.exceptions;

public class ECommandLine extends EReleaserException {

	private static final long serialVersionUID = 1L;
	
	public ECommandLine(String message) {
		super(message);
	}
	
	public ECommandLine(String message, Throwable t) {
		super(message, t);
	}

	public ECommandLine(Exception e) {
		super(e);
	}

	
}
