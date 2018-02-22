package org.scm4j.releaser.exceptions;

public class ENoDelayedTags extends EReleaserException {

	private static final long serialVersionUID = 1L;

	public ENoDelayedTags(String url) {
		super("No delayed tags for url " + url);
	}

}
