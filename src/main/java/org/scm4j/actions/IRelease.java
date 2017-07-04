package org.scm4j.actions;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.vcs.api.VCSTag;

public interface IRelease {

	VCSTag tagRelease(IProgress progress, String tagMessage);
	
	String getNewVersion();
	
	String getNewBranchName();

}
