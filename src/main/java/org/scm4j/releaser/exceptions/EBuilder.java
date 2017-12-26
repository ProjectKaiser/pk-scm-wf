package org.scm4j.releaser.exceptions;

import org.scm4j.releaser.conf.Component;

public class EBuilder extends EReleaserException {

	private static final long serialVersionUID = 1L;
	private final Component comp;

	public EBuilder(String message, Component comp) {
		super(message);
		this.comp = comp;
	}

	public Component getComp() {
		return comp;
	}
}
