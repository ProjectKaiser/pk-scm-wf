package org.scm4j.releaser.exceptions;

public class ENoRevisionsToTag extends EReleaserException {

	private static final long serialVersionUID = 1L;

	public ENoRevisionsToTag() {
		super("No revisions to tag");
	}

}
