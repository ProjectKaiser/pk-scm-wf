package org.scm4j.releaser.branch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.conf.Options;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.exceptions.EVCSBranchNotFound;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

public class MDepsSource {
	public static final File RELEASES_DIR = new File(System.getProperty("user.dir"), "releases");
	
	private List<Component> mdeps;
	private final Component comp;
	private final boolean hasCRB;
	private final String crbName;
	private final String rbName;
	private final Version crbVersion;
	private final Version rbVersion;
	private final Version devVersion;

	public List<Component> getMDeps() {
		if (mdeps != null) {
			return mdeps;
		}
		// beware: UBL forked, UDB forked -> take UDB 18.0 -> error no releases
		if (Options.isPatch()) {
			mdeps = getMDepsRelease(comp, crbName);
		} else {
			mdeps = hasCRB && rbVersion.getPatch().equals(Utils.ZERO_PATCH) ? getMDepsRelease(comp, crbName) : getMDepsDevelop(comp);
		}
		return mdeps;
	}
	
	public List<Component> getCRBMDeps() {
		return getMDepsRelease(comp, crbName);
	}
	
	public MDepsSource(Component comp) {
		this.comp = comp;
		IVCS vcs = comp.getVCS();
		devVersion = Utils.getDevVersion(comp);
		if (comp.getVersion().isLocked()) {
			rbName = Utils.getReleaseBranchName(comp, comp.getVersion());
			rbVersion = new Version(vcs.getFileContent(rbName, SCMReleaser.VER_FILE_NAME, null)).toRelease(); //comp.getVersion();
			crbName = Utils.getReleaseBranchName(comp, devVersion.toPreviousMinor());
			crbVersion = new Version(vcs.getFileContent(crbName, SCMReleaser.VER_FILE_NAME, null)).toRelease();
			hasCRB = true;
		} else {
			Boolean hasCRB;
			Version crbVersion;
			String releaseBranchName = Utils.getReleaseBranchName(comp, getDevVersion().toPreviousMinor());
			try {
				crbVersion = new Version(vcs.getFileContent(releaseBranchName, SCMReleaser.VER_FILE_NAME, null)).toRelease();
				hasCRB = true;
			} catch (EVCSBranchNotFound e) {
				crbVersion = getDevVersion().toReleaseZeroPatch();
				hasCRB = false;
			}
			this.hasCRB = hasCRB;
			rbVersion = crbVersion;
			this.crbVersion = crbVersion;
			crbName = hasCRB ? releaseBranchName : null;
			rbName = crbName;
		}
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

	public String getCrbName() {
		return crbName;
	}

	public boolean hasCRB() {
		return hasCRB;
	}

	public Version getCrbVersion() {
		return crbVersion;
	}

	public Version getRbVersion() {
		return rbVersion;
	}

	@Override
	public String toString() {
		return "MDepsSource [mdeps=" + mdeps + ", comp=" + comp + ", hasCRB=" + hasCRB + ", crbName=" + crbName
				+ ", crbVersion=" + crbVersion + ", rbVersion=" + rbVersion + "]";
	}

	public Version getDevVersion() {
		return devVersion;
	}

	public String getRbName() {
		return rbName;
	}
}