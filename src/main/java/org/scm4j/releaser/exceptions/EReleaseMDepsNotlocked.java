package org.scm4j.releaser.exceptions;

import org.scm4j.releaser.conf.Component;

import java.util.List;

public class EReleaseMDepsNotLocked extends EReleaserException {

	private final List<Component> nonLockedMDeps;

	public EReleaseMDepsNotLocked(List<Component> nonlockedMDeps) {
		super("mDeps not locked in release branch: "+ nonlockedMDeps);
		this.nonLockedMDeps = nonlockedMDeps;
	}

	public List<Component> getNonLockedMDeps() {
		return nonLockedMDeps;
	}
}
