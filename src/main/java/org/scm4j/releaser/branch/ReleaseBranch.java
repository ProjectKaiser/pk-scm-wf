package org.scm4j.releaser.branch;

import java.io.File;
import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.exceptions.EVCSBranchNotFound;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

public class ReleaseBranch {
	public static final File RELEASES_DIR = new File(System.getProperty("user.dir"), "releases");
	private final Version version; // exists ? head version : db.version.minor-1.zeroPatch
	private final String name;
	private final boolean exists;
	private final String url;
	private final IVCS vcs;

	public Version getVersion() {
		return version;
	}

	public String getName() {
		return name;
	}
	
	public ReleaseBranch(Component comp, Version exactVersion, Boolean exists) {
		this.url = comp.getVcsRepository().getUrl();
		this.vcs = comp.getVCS();
		name = getName(comp, exactVersion);
		this.version = exactVersion;
		this.exists = exists;
	}

	private static Version getDevVersion(Component comp) {
		return new Version(comp.getVCS().getFileContent(comp.getVcsRepository().getDevelopBranch(), SCMReleaser.VER_FILE_NAME, null));
	}

	public boolean exists() {
		return exists;
	}
	
	public List<Component> getMDeps() {
		return getMDepsFile().getMDeps();
	}

	public MDepsFile getMDepsFile() {
		try {
			String mDepsFileContent = vcs.getFileContent(name, SCMReleaser.MDEPS_FILE_NAME, null);
			return new MDepsFile(mDepsFileContent);
		} catch (EVCSFileNotFound e) {
			return new MDepsFile("");
		}
	}

	public static String getName(Component comp, Version forVersion) {
		return comp.getVcsRepository().getReleaseBranchPrefix() + forVersion.getReleaseNoPatchString();
	}
	
	@Override
	public String toString() {
		return "ReleaseBranch [url=" + url + ", version=" + version.toReleaseString() + ", name=" + name + "]";
	}
	
	public File getBuildDir() {
		File buildDir = new File(RELEASES_DIR, url.replaceAll("[^a-zA-Z0-9.-]", "_"));
		buildDir = new File(buildDir, getName().replaceAll("[^a-zA-Z0-9.-]", "_"));
		return buildDir;
	}

	public String getUrl() {
		return url;
	}
}