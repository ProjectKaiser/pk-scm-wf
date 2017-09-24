package org.scm4j.releaser;

import org.scm4j.commons.Version;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

import java.util.ArrayList;
import java.util.List;

public class CurrentReleaseBranch {

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

	public CurrentReleaseBranch(Component comp) {
		this.comp = comp;
		vcs = comp.getVCS();
		version = getDevVersion().toPreviousMinor().setPatch("0").toRelease();
		name = comp.getVcsRepository().getReleaseBranchPrefix() + version.getReleaseNoPatchString();
	}

	public Component getComponent() {
		return comp;
	}

	private Version getDevVersion() {
		return new Version(vcs.getFileContent(comp.getVcsRepository().getDevBranch(), SCMReleaser.VER_FILE_NAME, null));
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
}