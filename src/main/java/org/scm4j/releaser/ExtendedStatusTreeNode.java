package org.scm4j.releaser;

import org.scm4j.commons.Version;
import org.scm4j.releaser.conf.Component;

import java.util.LinkedHashMap;

public class ExtendedStatusTreeNode {

	public static final ExtendedStatusTreeNode DUMMY = new ExtendedStatusTreeNode(null, null, null, null);
	
	private final Component comp;
	private final Version latestVersion;
	private final BuildStatus status;
	private final LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents;

	public ExtendedStatusTreeNode(Version latestVersion, BuildStatus status,
			LinkedHashMap<Component, ExtendedStatusTreeNode> subComponents, Component comp) {
		this.latestVersion = latestVersion;
		this.status = status;
		this.subComponents = subComponents;
		this.comp = comp;
	}
	
	public Version getLatestVersion() {
		return latestVersion;
	}

	public BuildStatus getStatus() {
		return status;
	}

	public LinkedHashMap<Component, ExtendedStatusTreeNode> getSubComponents() {
		return subComponents;
	}

	public Component getComp() {
		return comp;
	}
	
	@Override
	public String toString() {
		return "ExtendedStatusTreeNode [comp=" + getComp() + ", latestVersion=" + latestVersion + ", status=" + status
				+ ", subComponents=" + subComponents + "]";
	}
}
