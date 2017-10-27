package org.scm4j.releaser.scmactions;

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
		progress.startTrace("reading mdeps to actualize patches... ");
		MDepsFile currentMDepsFile = rb.getMDepsFile();
		progress.endTrace("done");
		StringBuilder sb = new StringBuilder();
		boolean hasNew = false;
		ReleaseBranch rbMDep;
		Version newVersion;
		for (Component currentMDep : currentMDepsFile.getMDeps()) {
			progress.startTrace("determining Release Branch version for mdep " + currentMDep + "... ");
			rbMDep = new ReleaseBranch(currentMDep);
			progress.endTrace("done");
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
			progress.startTrace("actualizing patches... ");
			vcs.setFileContent(rb.getName(), SCMReleaser.MDEPS_FILE_NAME, currentMDepsFile.toFileContent(), LogTag.SCM_MDEPS);
			progress.endTrace("done");
		} else {
			progress.reportStatus("mdeps patches are actual already");
		}

	}

}
