package org.scm4j.wf;

import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.wf.conf.Credentials;
import org.scm4j.wf.conf.VCSType;

public interface IVCSFactory {
	IVCS getVCS(VCSType type, Credentials creds, String url, IVCSWorkspace ws);
}
