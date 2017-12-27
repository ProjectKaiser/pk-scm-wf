package org.scm4j.releaser.scmactions.procs;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.conf.VCSRepositoryFactory;
import org.scm4j.vcs.api.IVCS;

public class SCMProcLockMDeps implements ISCMProc {
	
	private final IVCS vcs;
	private final ExtendedStatus status;
	private final CachedStatuses cache;
	private final VCSRepositoryFactory repoFactory;
	private final VCSRepository repo;

	public SCMProcLockMDeps(CachedStatuses cache, VCSRepositoryFactory repoFactory, VCSRepository repo) {
		this.repo = repo;
		status = cache.get(repo.getUrl());
		vcs = repo.getVCS();
		this.cache = cache;
		this.repoFactory = repoFactory;
	}

	@Override
	public void execute(IProgress progress) {
		ReleaseBranchFactory.getCRB(repo);
		if (status.getSubComponents().isEmpty()) {
			progress.reportStatus("no mdeps to lock");
			return;
		}
		String rbName = Utils.getReleaseBranchName(repo, status.getNextVersion());
		MDepsFile currentMDepsFile = new MDepsFile(vcs.getFileContent(rbName, Utils.MDEPS_FILE_NAME, null));

		StringBuilder sb = new StringBuilder();
		Version newVersion;
		for (Component currentMDep : currentMDepsFile.getMDeps()) {
			newVersion = cache.get(repoFactory.getUrl(currentMDep)).getNextVersion();
			if (!newVersion.getPatch().equals(Utils.ZERO_PATCH)) {
				newVersion = newVersion.toPreviousPatch();
			}
			sb.append("" + currentMDep.getName() + ": " + currentMDep.getVersion() + " -> " + newVersion + "\r\n");
			currentMDepsFile.replaceMDep(currentMDep.clone(newVersion));
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 2);
			progress.reportStatus("mdeps to lock:\r\n" + sb.toString());
			Utils.reportDuration(() -> vcs.setFileContent(rbName, Utils.MDEPS_FILE_NAME, currentMDepsFile.toFileContent(), LogTag.SCM_MDEPS),
					"lock mdeps" , null, progress);
		}
	}
}
