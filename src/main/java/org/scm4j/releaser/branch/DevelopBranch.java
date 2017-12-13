package org.scm4j.releaser.branch;

import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.releaser.ActionTreeBuilder;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.exceptions.EComponentConfig;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

public class DevelopBranch {
	
	private final Component comp;
	
	public DevelopBranch(Component comp) {
		this.comp = comp;
	}
	
	public boolean isModified() {
		List<VCSCommit> log = comp.getVCS().log(comp.getVcsRepository().getDevelopBranch(), 1);
		if (log.isEmpty()) {
			return false;
		}
		VCSCommit lastCommit = log.get(0);
		if (lastCommit.getLogMessage().contains(LogTag.SCM_IGNORE) || lastCommit.getLogMessage().contains(LogTag.SCM_VER)) {
			return false;
		}
		return true;
	}
	
	public Version getVersion() {
		try {
			String verFileContent = comp.getVCS().getFileContent(comp.getVcsRepository().getDevelopBranch(), ActionTreeBuilder.VER_FILE_NAME, null);
			return new Version(verFileContent.trim());
		} catch (EVCSFileNotFound e) {
			throw new EComponentConfig(ActionTreeBuilder.VER_FILE_NAME + " file is missing in develop branch of " + comp);
		}
	}
	
}
