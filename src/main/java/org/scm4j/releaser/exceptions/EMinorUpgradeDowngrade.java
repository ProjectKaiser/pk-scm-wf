package org.scm4j.releaser.exceptions;

import org.scm4j.commons.Version;
import org.scm4j.releaser.conf.Component;

public class EMinorUpgradeDowngrade extends EReleaserException {

	private static final long serialVersionUID = 1L;

	private final Version changeToVersion;
	private final Component problematicMDep;
	private final Component rootComp;

	public EMinorUpgradeDowngrade(Component rootComp, Component problematicMDep, Version changeToVersion) {
		super(String.format("minor upgrade/downgrade detected for mdep %s of root comp %s: attempt to change to %s", problematicMDep, rootComp, changeToVersion));
		this.changeToVersion = changeToVersion;
		this.problematicMDep = problematicMDep;
		this.rootComp = rootComp;
	}

	public Version getChangeToVersion() {
		return changeToVersion;
	}

	public Component getProblematicMDep() {
		return problematicMDep;
	}

	public Component getRootComp() {
		return rootComp;
	}
}
