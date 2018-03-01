package org.scm4j.releaser.scmactions.procs;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.Constants;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.conf.VCSRepositoryFactory;
import org.scm4j.vcs.api.IVCS;

public class SCMProcActualizePatches implements ISCMProc {
	
	private final CachedStatuses cache;
	private final VCSRepositoryFactory repoFactory;
	private final VCSRepository repo;
 
	public SCMProcActualizePatches(CachedStatuses cache, VCSRepositoryFactory repoFactory, VCSRepository repo) {
		this.cache = cache;
		this.repoFactory = repoFactory;
		this.repo = repo;
	}

	@Override
	public void execute(IProgress progress) {
		IVCS vcs = repo.getVCS();
		MDepsFile currentMDepsFile = new MDepsFile(vcs.getFileContent(
				Utils.getReleaseBranchName(repo, cache.get(repo.getUrl()).getNextVersion()),
				Constants.MDEPS_FILE_NAME, null));
		StringBuilder sb = new StringBuilder();
		Version newVersion;
		for (Component currentMDep : currentMDepsFile.getMDeps()) {
			newVersion = cache.get(repoFactory.getUrl(currentMDep)).getNextVersion();
			if (!newVersion.getPatch().equals(Constants.ZERO_PATCH)) {
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
			Utils.reportDuration(() -> vcs.setFileContent(Utils.getReleaseBranchName(repo, cache.get(repo.getUrl()).getNextVersion()), Constants.MDEPS_FILE_NAME, currentMDepsFile.toFileContent(), Constants.SCM_MDEPS),
					"writting mdeps", null, progress);
		} else {
			progress.reportStatus("mdeps patches are actual already");
		}
	}
}
