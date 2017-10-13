package org.scm4j.releaser.exceptions;

public class EConfig extends EReleaserException {

	private static final long serialVersionUID = 1L;

	public EConfig(String message) {
		super(message);
	}

	public EConfig(String message, Exception e) {
		super(message, e);
	}

}
