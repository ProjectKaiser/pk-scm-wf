package org.scm4j.wf;

import org.scm4j.vcs.GitVCS;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSRepositoryWorkspace;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.svn.SVNVCS;

public class IVCSFactory {
	
	public static IVCS getIVCS(IVCSWorkspace ws, VCSRepository repo) {
		IVCSRepositoryWorkspace rws = ws.getVCSRepositoryWorkspace(repo.getUrl());
		IVCS vcs;
		switch (repo.getType()) {
		case GIT: {
			vcs = new GitVCS(rws);
			break;
		}
		case SVN: {
			vcs = new SVNVCS(rws, repo.getCredentials().getName(),
					repo.getCredentials().getPassword());
			break;
		}
		default: {
			throw new RuntimeException("Unsupported VCSTtype for repository: " + repo.toString());
		}
		}
		return vcs;
	}
}
