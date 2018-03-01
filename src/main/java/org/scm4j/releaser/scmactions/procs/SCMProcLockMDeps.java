package org.scm4j.releaser.scmactions.procs;

import org.apache.commons.lang3.StringUtils;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.*;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.conf.VCSRepositoryFactory;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSChangeListNode;

import java.util.ArrayList;
import java.util.List;

public class SCMProcLockMDeps implements ISCMProc {
	
	private final IVCS vcs;
	private final ExtendedStatus status;
	private final CachedStatuses cache;
	private final VCSRepositoryFactory repoFactory;
	private final VCSRepository repo;
	private final List<VCSChangeListNode> vcsChangeList;

	public SCMProcLockMDeps(CachedStatuses cache, VCSRepositoryFactory repoFactory, VCSRepository repo,
							List<VCSChangeListNode> vcsChangeList) {
		this.repo = repo;
		this.vcsChangeList = vcsChangeList;
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
			if (!vcsChangeList.isEmpty()) {
				String rbName = Utils.getReleaseBranchName(repo, status.getNextVersion());
				commitChangeList(rbName, "-SNAPSHOT truncate", progress);
			}
			return;
		}
		String rbName = Utils.getReleaseBranchName(repo, status.getNextVersion());
		MDepsFile currentMDepsFile = new MDepsFile(vcs.getFileContent(rbName, Constants.MDEPS_FILE_NAME, null));

		StringBuilder sb = new StringBuilder();
		Version newVersion;
		for (Component currentMDep : currentMDepsFile.getMDeps()) {
			newVersion = cache.get(repoFactory.getUrl(currentMDep)).getNextVersion();
			if (!newVersion.getPatch().equals(Constants.ZERO_PATCH)) {
				newVersion = newVersion.toPreviousPatch();
			}
			sb.append("" + currentMDep.getName() + ": " + currentMDep.getVersion() + " -> " + newVersion + "\r\n");
			currentMDepsFile.replaceMDep(currentMDep.clone(newVersion));
		}
		List<String> statusMessages = new ArrayList<>();
		if (!vcsChangeList.isEmpty()) {
			statusMessages.add("-SNAPSHOT truncate");
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 2);
			progress.reportStatus("mdeps to lock:\r\n" + sb.toString());
			statusMessages.add("lock mdeps");
			vcsChangeList.add(new VCSChangeListNode(Constants.MDEPS_FILE_NAME, currentMDepsFile.toFileContent(),
					Constants.SCM_MDEPS));
		}

		commitChangeList(rbName, StringUtils.join(statusMessages, ", "), progress);
	}

	private void commitChangeList(String branchName, String statusMessage, IProgress progress) {
		Utils.reportDuration(() -> vcs.setFileContent(branchName, vcsChangeList),
				statusMessage, null, progress);
}
}
