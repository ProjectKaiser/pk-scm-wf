package org.scm4j.releaser.branch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.exceptions.EVCSBranchNotFound;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

public class MDepsSource {
	public static final File RELEASES_DIR = new File(System.getProperty("user.dir"), "releases");
	
	private List<Component> mdeps;
	private final Component comp;
	private final Version version;
	private final boolean isFromDevelop;
	private final String name;
	private final Version devVersion;

	public List<Component> getMDeps() {
		if (mdeps != null) {
			return mdeps;
		}
		mdeps = isFromDevelop ? getMDepsDevelop(comp) : getMDepsRelease(comp, getName());
		return mdeps;
	}
	
	public MDepsSource(Component comp) {
		this.comp = comp;
		IVCS vcs = comp.getVCS();
		if (comp.getVersion().isLocked()) {
			name = Utils.getReleaseBranchName(comp, comp.getVersion());
			version = new Version(vcs.getFileContent(name, SCMReleaser.VER_FILE_NAME, null)).toRelease();
			isFromDevelop = false;
			devVersion = null;
		} else {
			Boolean crbExists;
			devVersion = Utils.getDevVersion(comp);
			Version crbVersion;
			String releaseBranchName = Utils.getReleaseBranchName(comp, getDevVersion().toPreviousMinor());
			try {
				crbVersion = new Version(vcs.getFileContent(releaseBranchName, SCMReleaser.VER_FILE_NAME, null)).toRelease();
				crbExists = true;
			} catch (EVCSBranchNotFound e) {
				crbVersion = getDevVersion().toReleaseZeroPatch();
				crbExists = false;
			}
			
			if (crbExists && crbVersion.getPatch().equals("0")) {
				isFromDevelop = false;
			} else {
				isFromDevelop = true;
			}
			
			this.version = crbVersion;
			name = isFromDevelop ? null : releaseBranchName;
		}
	}

	public Version getVersion() {
		return version;
	}
	
	public static List<Component> getMDepsRelease(Component comp, String releaseBranchName) {
		try {
			String mDepsFileContent = comp.getVCS().getFileContent(releaseBranchName, SCMReleaser.MDEPS_FILE_NAME, null);
			return new MDepsFile(mDepsFileContent).getMDeps();
		} catch (EVCSFileNotFound e) {
			return new ArrayList<>();
		}
	}
	
	public static List<Component> getMDepsDevelop(Component comp) {
		List<Component> res = new ArrayList<>();
		for (Component mDep : getMDepsRelease(comp, null)) {
			res.add(mDep.clone(""));
		}
		return res;
	}

	public String getName() {
		return name;
	}

	public boolean isDevelopMDepSource() {
		return isFromDevelop;
	}
	
	public Version getDevVersion() {
		return devVersion;
	}

	@Override
	public String toString() {
		return "WorkingBranch [comp=" + comp + ", version=" + version + ", name=" + name
				+ ", isDevelopMDepSource=" + isFromDevelop + ", mdeps=" + mdeps + "]";
	}
}