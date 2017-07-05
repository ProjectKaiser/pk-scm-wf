package org.scm4j.wf;

import org.scm4j.vcs.GitVCS;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.svn.SVNVCS;
import org.scm4j.wf.conf.VCSRepository;

public class VCSFactory {

	public static IVCS getIVCS(VCSRepository repo, IVCSWorkspace ws) {
		IVCS vcs;
		switch (repo.getType()) {
		case GIT: {
			vcs = new GitVCS(ws.getVCSRepositoryWorkspace(repo.getUrl()));
			break;
		}
		case SVN: {
			vcs = new SVNVCS(ws.getVCSRepositoryWorkspace(repo.getUrl()),
					repo.getCredentials() == null ? null : repo.getCredentials().getName(),
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
