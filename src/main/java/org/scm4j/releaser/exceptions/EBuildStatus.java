package org.scm4j.releaser.exceptions;

import org.scm4j.releaser.conf.Component;

public class EBuildStatus extends EReleaserException {
	
	private static final long serialVersionUID = 1L;
	private final Component comp;

	public EBuildStatus(Exception e, Component comp) {
		super(e);
		this.comp = comp;
	}
	
	@Override
	public String getMessage() {
		return String.format("Status build failed for %s: %s", comp.toString(), getCause().getMessage());
	}

	public Component getComp() {
		return comp;
	}
}
