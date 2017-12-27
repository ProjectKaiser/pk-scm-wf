package org.scm4j.releaser.branch;

import java.util.ArrayList;
import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.vcs.api.exceptions.EVCSBranchNotFound;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

public final class ReleaseBranchFactory {
	
	public static ReleaseBranchPatch getReleaseBranchPatch(Version patchVersion, VCSRepository repo) {
		String name = Utils.getReleaseBranchName(repo, patchVersion);
		boolean exists;
		Version version;
		List<Component> mdeps;
		try {
			version = new Version(repo.getVCS().getFileContent(name, Utils.VER_FILE_NAME, null)).toRelease();
			exists = true;
			mdeps = getMDepsRelease(name, repo);
		} catch (EVCSBranchNotFound e) {
			exists = false;
			version = null;
			mdeps = new ArrayList<>(); // will not be used because ENoReleaseBranchForPatch will be thrown 
		}
		
		return new ReleaseBranchPatch(mdeps, exists, name, version);
	}
	
	public static ReleaseBranchCurrent getCRB(VCSRepository repo) {
		Version devVersion = Utils.getDevVersion(repo);
		Version version;
		boolean exists;
		String name = Utils.getReleaseBranchName(repo, devVersion.toPreviousMinor());
		try {
			version = new Version(repo.getVCS().getFileContent(name, Utils.VER_FILE_NAME, null)).toRelease();
			exists = true;
		} catch (EVCSBranchNotFound e) {
			version = devVersion.toReleaseZeroPatch();
			exists = false;
		}
		List<Component> mdeps = exists && version.getPatch().equals(Utils.ZERO_PATCH) ? getMDepsRelease(name, repo) : getMDepsDevelop(repo);
	
		return new ReleaseBranchCurrent(mdeps, exists, name, version, devVersion);
	}
	
	public static List<Component> getMDepsRelease(String releaseBranchName, VCSRepository repo) {
		try {
			String mDepsFileContent = repo.getVCS().getFileContent(releaseBranchName, Utils.MDEPS_FILE_NAME, null);
			return new MDepsFile(mDepsFileContent).getMDeps();
		} catch (EVCSFileNotFound e) {
			return new ArrayList<>();
		}
	}
	
	public static List<Component> getMDepsDevelop(VCSRepository repo) {
		List<Component> res = new ArrayList<>();
		for (Component mDep : getMDepsRelease(null, repo)) {
			res.add(mDep.clone(""));
		}
		return res;
	}
}
