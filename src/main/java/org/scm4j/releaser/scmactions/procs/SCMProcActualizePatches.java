package org.scm4j.releaser.scmactions.procs;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.conf.VCSRepositoryFactory;
import org.scm4j.vcs.api.IVCS;

public class SCMProcActualizePatches implements ISCMProc {
	
	private final IVCS vcs;
	private final Component comp;
	private final CachedStatuses cache;
	private final VCSRepositoryFactory repoFactory;
 
	public SCMProcActualizePatches(Component comp, CachedStatuses cache, VCSRepositoryFactory repoFactory) {
		this.cache = cache;
		this.comp = comp;
		vcs = comp.getVCS();
		this.repoFactory = repoFactory;
	}

	@Override
	public void execute(IProgress progress) {
		MDepsFile currentMDepsFile = new MDepsFile(comp.getVCS().getFileContent(
				Utils.getReleaseBranchName(comp, cache.get(comp.getUrl()).getNextVersion()),
				Utils.MDEPS_FILE_NAME, null));//cache.get(comp.getUrl()).getSubComponents().keySet());
		//TODO: add workflow test mdeps file format saving
		StringBuilder sb = new StringBuilder();
		Version newVersion;
		for (Component currentMDep : currentMDepsFile.getMDeps(repoFactory)) {
			newVersion = cache.get(currentMDep.getUrl()).getNextVersion();
			if (!newVersion.getPatch().equals(Utils.ZERO_PATCH)) {
				newVersion = newVersion.toPreviousPatch();
			}
			if (!newVersion.equals(currentMDep.getVersion())) {
				sb.append("" + currentMDep.getName() + ": " + currentMDep.getVersion() + " -> " + newVersion + "\r\n");
				currentMDepsFile.replaceMDep(currentMDep.clone(newVersion));
			}
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 2);
			progress.reportStatus("patches to actualize:\r\n" + sb.toString());
			Utils.reportDuration(() -> vcs.setFileContent(Utils.getReleaseBranchName(comp, cache.get(comp.getUrl()).getNextVersion()), Utils.MDEPS_FILE_NAME, currentMDepsFile.toFileContent(), LogTag.SCM_MDEPS),
					"writting mdeps", null, progress);
		} else {
			progress.reportStatus("mdeps patches are actual already");
		}
	}
}
