package org.scm4j.releaser.scmactions.procs;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.IVCS;

public class SCMProcActualizePatches implements ISCMProc {
	
	private final ReleaseBranch rb;
	private final IVCS vcs;
 
	public SCMProcActualizePatches(ReleaseBranch rb) {
		this.rb = rb;
		vcs = rb.getComponent().getVCS();
	}

	@Override
	public void execute(IProgress progress) {
		MDepsFile currentMDepsFile = SCMReleaser.reportDuration(() -> rb.getMDepsFile(), "mdeps read to actualize patches", null, progress);
		StringBuilder sb = new StringBuilder();
		boolean hasNew = false;
		ReleaseBranch rbMDep;
		Version newVersion;
		for (Component currentMDep : currentMDepsFile.getMDeps()) {
			rbMDep = SCMReleaser.reportDuration(() ->  new ReleaseBranch(currentMDep), "release branch version calculation", currentMDep, progress);
			newVersion = rbMDep.getVersion().toPreviousPatch(); // TODO: which patch of mdep to actualize on if there are no mdep release branches at all?
			if (!newVersion.equals(currentMDep.getVersion())) {
				hasNew = true;
			}
			
			if (hasNew) {
				sb.append("" + currentMDep.getName() + ": " + currentMDep.getVersion() + " -> " + newVersion + "\r\n");
			}
			currentMDepsFile.replaceMDep(currentMDep.clone(newVersion));
		}
		if (hasNew) {
			sb.setLength(sb.length() - 2);
			progress.reportStatus("patches to actualize:\r\n" + sb.toString());
			SCMReleaser.reportDuration(() -> vcs.setFileContent(rb.getName(), SCMReleaser.MDEPS_FILE_NAME, currentMDepsFile.toFileContent(), LogTag.SCM_MDEPS), 
					"writting mdeps", null, progress);
		} else {
			progress.reportStatus("mdeps patches are actual already");
		}

	}

}
