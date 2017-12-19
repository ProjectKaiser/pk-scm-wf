package org.scm4j.releaser.exceptions;

import org.scm4j.releaser.conf.Component;

public class EBuildOnNotForkedRelease extends EReleaserException {

	private static final long serialVersionUID = 1L;
	private final Component comp;

	public EBuildOnNotForkedRelease(Component comp) {
		super("Can not build a not forked component " + comp + ". Fork it first explicitly.");
		this.comp = comp;
	}

	public Component getComp() {
		return comp;
	}
}
