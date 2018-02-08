package org.scm4j.releaser.exceptions;

import org.scm4j.releaser.conf.Component;

public class EInconsistentCompState extends EReleaserException {

	private static final long serialVersionUID = 1L;
	private final Component comp;

	public EInconsistentCompState(Component comp, String description) {
		super("Inconsistent " + comp + " state: " + description);
		this.comp = comp;
	}

	public Component getComp() {
		return comp;
	}
}
