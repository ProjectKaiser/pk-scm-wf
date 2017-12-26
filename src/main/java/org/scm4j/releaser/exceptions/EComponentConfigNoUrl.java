package org.scm4j.releaser.exceptions;

public class EComponentConfigNoUrl extends EReleaserException {

	private static final long serialVersionUID = 1L;
	
	public EComponentConfigNoUrl(String compName) {
		super("no repo url for " + compName);
	}

}
