package org.scm4j.releaser;

import java.util.LinkedHashMap;
import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.commons.coords.Coords;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.branch.DevelopBranch;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.exceptions.EVCSBranchNotFound;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

public class ExtendedStatusTreeBuilder {
	
	public ExtendedStatusTreeNode getExtendedStatusTreeNode(Component comp) {
		return getExtendedStatusTreeNode(comp, new CalculatedResult(), new ProgressConsole());
	}
	
	
	public ExtendedStatusTreeNode getExtendedStatusTreeNode(Component comp, CalculatedResult calculatedResult, IProgress progress) {
		IVCS vcs = comp.getVCS();

		Version candidateVer = getDevVersion(comp).toPreviousMinor().toReleaseZeroPatch();
		Version tempVersion;
		boolean crbExists;
		try {
			tempVersion = new Version(vcs.getFileContent(getName(comp, candidateVer), SCMReleaser.VER_FILE_NAME, null)).toRelease();
			crbExists = true;
		} catch (EVCSBranchNotFound | EVCSFileNotFound e) {
			crbExists = false;
			tempVersion = candidateVer;
		}
		
		final Version latestVersion = tempVersion;
		
		List<Component> mdeps;
		if (crbExists && latestVersion.getPatch().equals("0")) {
			mdeps = calculatedResult.setMDeps(comp, () -> new DevelopBranch(comp).getMDeps(), progress);
		} else {
			mdeps = calculatedResult.setMDeps(comp, () -> new ReleaseBranch(comp, latestVersion, true).getMDeps(), progress); 
		}
		
		LinkedHashMap<Coords, ExtendedStatusTreeNode> subComponents = new LinkedHashMap<>();
		for (Component mdep : mdeps) {
			subComponents.put(mdep.getCoords(), getExtendedStatusTreeNode(mdep));
		}
		
		BuildStatus status = calculatedResult.setBuildStatus(comp, () -> new Build(comp).getStatus(), progress);
		return new ExtendedStatusTreeNode(latestVersion, status, subComponents, comp.getCoords());
	}
	
	private Version getDevVersion(Component comp) {
		return new Version(comp.getVCS().getFileContent(comp.getVcsRepository().getDevelopBranch(), SCMReleaser.VER_FILE_NAME, null));
	}

	public String getName(Component comp, Version forVersion) {
		return comp.getVcsRepository().getReleaseBranchPrefix() + forVersion.getReleaseNoPatchString();
	}
}
