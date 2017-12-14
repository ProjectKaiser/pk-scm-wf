package org.scm4j.releaser.scmactions.procs;

import java.util.ArrayList;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.IVCS;

public class SCMProcFreezeMDeps implements ISCMProc {
	
	private final IVCS vcs;
	private final Component comp;
	private final ExtendedStatus status;
	private final CachedStatuses cache;
 
	public SCMProcFreezeMDeps(Component comp, CachedStatuses cache) {
		status = cache.get(comp.getUrl());
		this.comp = comp;
		vcs = comp.getVCS();
		this.cache = cache;
	}

	@Override
	public void execute(IProgress progress) {
		MDepsFile currentMDepsFile = new MDepsFile(new ArrayList<>(status.getSubComponents().keySet()));
		if (!currentMDepsFile.hasMDeps()) {
			progress.reportStatus("no mdeps to freeze");
			return;
		}
		StringBuilder sb = new StringBuilder();
		Version newVersion;
		boolean hasChanges = false;
		for (Component currentMDep : currentMDepsFile.getMDeps()) {
			newVersion = cache.get(currentMDep.getUrl()).getNextVersion();
			if (!newVersion.getPatch().equals(Utils.ZERO_PATCH)) {
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
			Utils.reportDuration(() -> vcs.setFileContent(Utils.getReleaseBranchName(comp, status.getNextVersion()), Utils.MDEPS_FILE_NAME, currentMDepsFile.toFileContent(), LogTag.SCM_MDEPS),
					"freeze mdeps" , null, progress);
		}
	}
}
