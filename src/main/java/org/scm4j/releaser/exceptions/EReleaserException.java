package org.scm4j.releaser.exceptions;

public class EReleaserException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public EReleaserException(Exception e) {
		super(e);
	}
	
	public EReleaserException(String message) {
		super(message);
	}
}
