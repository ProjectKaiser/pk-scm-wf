package org.scm4j.releaser.branch;

import org.scm4j.commons.Version;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

import java.util.ArrayList;
import java.util.List;

public class ReleaseBranch {

	private final Component comp;
	private final IVCS vcs;
	private final Version version;
	private final String name;

	public Version getVersion() {
		return version;
	}

	public String getName() {
		return name;
	}
	
	public ReleaseBranch(Component comp) {
		this(comp, getDevVersion(comp).toPreviousMinor().toRelease());
	}

	public ReleaseBranch(Component comp, Version exactVersion) {
		this.comp = comp;
		vcs = comp.getVCS();
		version = exactVersion;
		name = getName(comp, version);
	}

	public Component getComponent() {
		return comp;
	}
	
	private static Version getDevVersion(Component comp) {
		return new Version(comp.getVCS().getFileContent(comp.getVcsRepository().getDevBranch(), SCMReleaser.VER_FILE_NAME, null));
	}

	public boolean exists() {
		return vcs.getBranches(comp.getVcsRepository().getReleaseBranchPrefix()).contains(name);
	}

	public List<Component> getMDeps() {
		try {
			String mDepsFileContent = comp.getVCS().getFileContent(name, SCMReleaser.MDEPS_FILE_NAME, null);
			MDepsFile mDeps = new MDepsFile(mDepsFileContent);
			return mDeps.getMDeps();
		} catch (EVCSFileNotFound e) {
			return new ArrayList<>();
		}
	}

	public Version getHeadVersion() {
		return new Version(comp.getVCS().getFileContent(getName(), SCMReleaser.VER_FILE_NAME, null).trim());
	}

	public static String getName(Component comp, Version forVersion) {
		return comp.getVcsRepository().getReleaseBranchPrefix() + forVersion.getReleaseNoPatchString();
	}
	
	@Override
	public String toString() {
		return "ReleaseBranch [comp=" + comp + ", version=" + version.toReleaseString() + ", name=" + name + "]";
	}
}