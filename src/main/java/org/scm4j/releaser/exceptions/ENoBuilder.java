package org.scm4j.releaser.exceptions;

import org.scm4j.releaser.conf.Component;

public class ENoBuilder extends EBuilder {

	private static final long serialVersionUID = 1L;

	public ENoBuilder(Component comp) {
		super("no builder for " + comp, comp);
	}

}
