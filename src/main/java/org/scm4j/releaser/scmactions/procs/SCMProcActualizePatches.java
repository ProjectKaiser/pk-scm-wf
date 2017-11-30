package org.scm4j.releaser.scmactions.procs;

import java.util.ArrayList;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.IVCS;

public class SCMProcActualizePatches implements ISCMProc {
	
	private final IVCS vcs;
	private final Component comp;
	private final CachedStatuses cache;
 
	public SCMProcActualizePatches(Component comp, CachedStatuses cache) {
		this.cache = cache;
		this.comp = comp;
		vcs = comp.getVCS();
	}

	@Override
	public void execute(IProgress progress) {
		MDepsFile currentMDepsFile = new MDepsFile(new ArrayList<>(cache.get(comp.getUrl()).getSubComponents().keySet()));
		StringBuilder sb = new StringBuilder();
		boolean hasNew = false;
		Version newVersion;
		for (Component currentMDep : currentMDepsFile.getMDeps()) {
			newVersion = cache.get(currentMDep.getUrl()).getWBVersion().toPreviousPatch();
			if (!newVersion.equals(currentMDep.getVersion())) {
				hasNew = true;
			}
			
			if (hasNew) {
				sb.append("" + currentMDep.getName() + ": " + currentMDep.getVersion() + " -> " + newVersion + "\r\n");
			}
			currentMDepsFile.replaceMDep(currentMDep.clone(newVersion));
			// for what?
			//calculatedResult.replaceReleaseBranch(currentMDep, new ReleaseBranch(comp, rbMDep.getVersion(), true));
		}
		if (hasNew) {
			sb.setLength(sb.length() - 2);
			progress.reportStatus("patches to actualize:\r\n" + sb.toString());
			Utils.reportDuration(() -> vcs.setFileContent(Utils.getReleaseBranchName(comp, cache.get(comp.getUrl()).getWBVersion()), SCMReleaser.MDEPS_FILE_NAME, currentMDepsFile.toFileContent(), LogTag.SCM_MDEPS),
					"writting mdeps", null, progress);
		} else {
			progress.reportStatus("mdeps patches are actual already");
		}

	}

}
