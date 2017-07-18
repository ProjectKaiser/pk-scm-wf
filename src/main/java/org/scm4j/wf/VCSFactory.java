package org.scm4j.wf;

import org.scm4j.vcs.GitVCS;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.svn.SVNVCS;
import org.scm4j.wf.conf.Credentials;
import org.scm4j.wf.conf.VCSType;

public class VCSFactory implements IVCSFactory {

	@Override
	public IVCS getVCS(VCSType type, Credentials creds, String url, IVCSWorkspace ws) {
		IVCS vcs;
		switch (type) {
		case GIT: {
			vcs = new GitVCS(ws.getVCSRepositoryWorkspace(url));
			if (creds.getName() != null) {
					vcs.setCredentials(
							creds.getName(),
							creds.getPassword());
			}
			break;
		}
		case SVN: {
			vcs = new SVNVCS(ws.getVCSRepositoryWorkspace(url),
					creds == null ? null : creds.getName(),
					creds == null ? null : creds.getPassword());
			break;
		}
		default: {
			throw new RuntimeException("Unsupported VCSTtype for repository: " + url);
		}
		}
		return vcs;
	}
}
