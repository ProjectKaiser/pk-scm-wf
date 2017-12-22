package org.scm4j.releaser.conf;

import org.scm4j.commons.RegexConfig;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;

public interface IConfig {
	
	RegexConfig getRepoConfig();
	RegexConfig getCredentialsConfig();
	IVCSWorkspace getWS();
	Boolean isConfiguredByEnvironment();

}
