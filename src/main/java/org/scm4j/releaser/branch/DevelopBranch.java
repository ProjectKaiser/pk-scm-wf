package org.scm4j.releaser.branch;

import org.scm4j.commons.Version;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.exceptions.ENoVersionFile;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

import java.util.List;

public class DevelopBranch {

	private final VCSRepository repo;
	private final Component comp;
	
	public DevelopBranch(Component comp, VCSRepository repo) {
		this.comp = comp;
		this.repo = repo;
	}
	
	public boolean isModified() {
		List<VCSCommit> log = repo.getVCS().log(repo.getDevelopBranch(), 1);
		if (log.isEmpty()) {
			return false;
		}
		VCSCommit lastCommit = log.get(0);
		return !(lastCommit.getLogMessage().contains(LogTag.SCM_IGNORE) || lastCommit.getLogMessage().contains(LogTag.SCM_VER));
	}
	
	public Version getVersion() {
		try {
			String verFileContent = repo.getVCS().getFileContentFromBranch(repo.getDevelopBranch(), Utils.VER_FILE_NAME);
			return new Version(verFileContent.trim());
		} catch (EVCSFileNotFound e) {
			throw new ENoVersionFile(comp);
		}
	}
}
