package org.scm4j.wf;

import org.scm4j.vcs.GitVCS;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.svn.SVNVCS;

public class IVCSFactory {
	
	public static IVCS getIVCS(VCSRepository repo) {
		IVCS vcs;
		switch (repo.getType()) {
		case GIT: {
			vcs = new GitVCS(repo.getWorkspace());
			break;
		}
		case SVN: {
			vcs = new SVNVCS(repo.getWorkspace(), repo.getCredentials() == null ? null : repo.getCredentials().getName(),
					repo.getCredentials() == null ? null : repo.getCredentials().getPassword());
			break;
		}
		default: {
			throw new RuntimeException("Unsupported VCSTtype for repository: " + repo.toString());
		}
		}
		return vcs;
	}
}
