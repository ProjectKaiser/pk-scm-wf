package org.scm4j.releaser.scmactions.procs;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.*;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.IVCS;

public class SCMProcFreezeMDeps implements ISCMProc {
	
	private final ReleaseBranch rb;
	private final IVCS vcs;
	private final CalculatedResult calculatedResult;
	private final Component comp;
 
	public SCMProcFreezeMDeps(ReleaseBranch rb, Component comp, CalculatedResult calculatedResult) {
		this.rb = rb;
		this.calculatedResult = calculatedResult;
		this.comp = comp;
		vcs = comp.getVCS();
	}

	@Override
	public void execute(IProgress progress) {
		MDepsFile currentMDepsFile = new MDepsFile(calculatedResult.setMDeps(comp,
				rb::getMDeps, progress));
		if (!currentMDepsFile.hasMDeps()) {
			progress.reportStatus("no mdeps to freeze");
			return;
		}
		StringBuilder sb = new StringBuilder();
		ReleaseBranch rbMDep;
		Version newVersion;
		boolean hasChanges = false;
		for (Component currentMDep : currentMDepsFile.getMDeps()) {
			rbMDep = calculatedResult.setReleaseBranch(currentMDep, () -> new ReleaseBranch(currentMDep), progress);
			// untilldb is built -> rbMDep.getVersion is 2.59.1, but we need 2.59.0
			newVersion = rbMDep.getVersion();
			if (!newVersion.getPatch().equals(Build.ZERO_PATCH)) {
				newVersion = newVersion.toPreviousPatch();
			}
			sb.append("" + currentMDep.getName() + ": " + currentMDep.getVersion() + " -> " + newVersion + "\r\n");
			currentMDepsFile.replaceMDep(currentMDep.clone(newVersion));
			hasChanges = true;
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 2);
		}
		if (hasChanges) {
			progress.reportStatus("mdeps to freeze:\r\n" + sb.toString());
			Utils.reportDuration(() -> vcs.setFileContent(rb.getName(), SCMReleaser.MDEPS_FILE_NAME, currentMDepsFile.toFileContent(), LogTag.SCM_MDEPS),
					"freeze mdeps" , null, progress);
		}
	}
}
