package org.scm4j.releaser;

import org.scm4j.commons.Version;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.VCSRepository;

import java.util.LinkedHashMap;

public class ExtendedStatus {

	public static final ExtendedStatus DUMMY = new ExtendedStatus(null, null, null, null, null);
	
	private final Component comp;
	private final Version nextVersion;
	private final BuildStatus status;
	private final LinkedHashMap<Component, ExtendedStatus> subComponents;
	private final VCSRepository repo;
	private final String errorDesc;
	
	public ExtendedStatus(Version nextVersion, BuildStatus status,
			  LinkedHashMap<Component, ExtendedStatus> subComponents, Component comp, VCSRepository repo) {
		this(nextVersion, status, subComponents, comp, repo, null);
	}

	public ExtendedStatus(Version nextVersion, BuildStatus status,
						  LinkedHashMap<Component, ExtendedStatus> subComponents, Component comp, VCSRepository repo, String errorDesc) {
		this.nextVersion = nextVersion;
		this.status = status;
		this.subComponents = subComponents;
		this.comp = comp;
		this.repo = repo;
		this.errorDesc = errorDesc;
	}
	
	public Version getNextVersion() {
		return nextVersion;
	}

	public BuildStatus getStatus() {
		return status;
	}

	public LinkedHashMap<Component, ExtendedStatus> getSubComponents() {
		return subComponents;
	}

	public Component getComp() {
		return comp;
	}
	
	public String getErrorDesc() {
		return errorDesc;
	}
	
	@Override
	public String toString() {
		if (this == DUMMY) {
			return "<DUMMY>";
		}
		if (status == BuildStatus.ERROR) {
 			return String.format("%s %s: %s", status, comp.getCoords(), getErrorDesc());
 		}
 		String targetBranch = Utils.getReleaseBranchName(repo, nextVersion);
 		return String.format("%s %s, target version: %s, target branch: %s", status, comp.getCoords(), nextVersion, targetBranch);
	}
}
