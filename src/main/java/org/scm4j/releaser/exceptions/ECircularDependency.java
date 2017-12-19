package org.scm4j.releaser.exceptions;

import org.scm4j.releaser.conf.Component;

public class ECircularDependency extends EReleaserException {
	
	private static final long serialVersionUID = 1L;

	public ECircularDependency(Component comp) {
		super("circular dependency is detected for " + comp);
	}
}
