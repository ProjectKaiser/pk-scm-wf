package org.scm4j.releaser;

import java.util.ArrayList;
import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.releaser.conf.Component;
import org.scm4j.vcs.api.IVCS;

public class CurrentReleaseBranch {
	
	private final Component comp;
	private final IVCS vcs;
	
	public CurrentReleaseBranch(Component comp) {
		this.comp = comp;
		vcs = comp.getVCS();
	}
	
	public Version getDevVersion() {
		return new Version(vcs.getFileContent(comp.getVcsRepository().getDevBranch(), SCMReleaser.VER_FILE_NAME, null));
	}
	
	public boolean exists() {
		List<String> branches = new ArrayList<>(vcs.getBranches(comp.getVcsRepository().getReleaseBranchPrefix()));
		return branches.contains(comp.getVcsRepository().getReleaseBranchPrefix() + getVersion().getReleaseNoPatchString());
	}

	public Version getVersion() {
		return getDevVersion().toPreviousMinor().toRelease();
	}

	public List<Component> getMDeps() {
		// TODO Auto-generated method stub
		return null;
	}
	
}