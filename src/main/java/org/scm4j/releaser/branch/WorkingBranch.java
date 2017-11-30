package org.scm4j.releaser.branch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.scm4j.commons.Version;
import org.scm4j.releaser.ExtendedStatusTreeNode;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.exceptions.EVCSBranchNotFound;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

public class WorkingBranch {
	public static final File RELEASES_DIR = new File(System.getProperty("user.dir"), "releases");
	
	private List<Component> mdeps;
	private final Component comp;
	private final Version version;
	private final boolean isDevelopMDepSource;
	private final String name;

	public List<Component> getMDeps() {
		if (mdeps != null) {
			return mdeps;
		}
		mdeps = isDevelop ? getMDepsDevelop(comp) : getMDepsRelease(comp, name);
		return mdeps;
	}
	
	public WorkingBranch(Component comp) {
		this.comp = comp;
		IVCS vcs = comp.getVCS();
		if (comp.getVersion().isLocked()) {
			version = comp.getVersion();
			isDevelop = false;
			name = Utils.getReleaseBranchName(comp, version);
		} else {
			Version devVersion = Utils.getDevVersion(comp);
			Version version;
			Boolean isDevelop;
			Boolean crbExists;
			String releaseBranchName = Utils.getReleaseBranchName(comp, devVersion.toPreviousMinor());
			try {
				version = new Version(vcs.getFileContent(releaseBranchName, SCMReleaser.VER_FILE_NAME, null)).toRelease();
				crbExists = true;
			} catch (EVCSBranchNotFound e) {
				crbExists = false;
				
			}
			if (crbExists) {
				isDevelop = true;
			} else {
				isDevelop = false;
				version = devVersion;
			}
			version = devVersion.toPreviousMinor().toReleaseZeroPatch();
			this.isDevelop = isDevelop;
			this.version = version;
			if (isDevelop) {
				name = null;
			} else {
				name = releaseBranchName;
			}
		}
	}

	public Map<Component, ExtendedStatusTreeNode> getSubComponents() {
		return null;
	}
	
	public boolean isDevelop() {
		return isDevelop;
	}

	public Version getVersion() {
		return version;
	}
	
	public static List<Component> getMDepsRelease(Component comp, String releaseBranchName) {
		try {
			String mDepsFileContent = comp.getVCS().getFileContent(null, SCMReleaser.MDEPS_FILE_NAME, null);
			return new MDepsFile(mDepsFileContent).getMDeps();
		} catch (EVCSFileNotFound e) {
			return new ArrayList<>();
		}
	}
	
	public static List<Component> getMDepsDevelop(Component comp) {
		List<Component> res = getMDepsRelease(comp, null);
		for (Component mDep : res) {
			res.add(mDep.clone(""));
		}
		return res;
	}
}