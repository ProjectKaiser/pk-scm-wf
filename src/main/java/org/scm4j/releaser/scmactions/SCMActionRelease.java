package org.scm4j.releaser.scmactions;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.conf.VCSRepositoryFactory;
import org.scm4j.releaser.scmactions.procs.*;
import org.scm4j.vcs.api.VCSChangeListNode;

import java.util.ArrayList;
import java.util.List;

public class SCMActionRelease extends ActionAbstract {

	private final List<ISCMProc> procs = new ArrayList<>();
	private final BuildStatus bsFrom;
	private final BuildStatus bsTo;
	private final Version targetVersion;

	public SCMActionRelease(Component comp, List<IAction> childActions, CachedStatuses cache, VCSRepositoryFactory repoFactory,
							ActionSet actionSet, boolean delayedTag, VCSRepository repo) {
		super(comp, childActions, repo);
		ExtendedStatus status = cache.get(repo.getUrl());
		this.bsFrom = status.getStatus();
		targetVersion = status.getNextVersion();
		List<VCSChangeListNode> vcsChangeList = new ArrayList<>();
		BuildStatus bsTo = null;
		switch(bsFrom) {
		case FORK:
			procs.add(new SCMProcForkBranch(comp, cache, repo, vcsChangeList));
		case LOCK:
			getProcs().add(new SCMProcLockMDeps(cache, repoFactory, repo, vcsChangeList));
			bsTo = BuildStatus.LOCK;
			if (actionSet == ActionSet.FORK_ONLY) {
				break;
			}
		case BUILD_MDEPS:
		case ACTUALIZE_PATCHES:
			if (bsFrom.ordinal() > BuildStatus.LOCK.ordinal() && actionSet == ActionSet.FULL) {
				getProcs().add(new SCMProcActualizePatches(cache, repoFactory, repo));
			}
		case BUILD:
			if (actionSet == ActionSet.FULL) {
				getProcs().add(new SCMProcBuild(comp, cache, delayedTag, repo));
				bsTo = BuildStatus.BUILD;
			}
		case DONE:
			break;
		default:
			throw new IllegalArgumentException("unsupported build status: " + bsFrom);
		}
		this.bsTo = bsTo;
	}

	@Override
	protected void executeAction(IProgress progress) {
		for (ISCMProc proc : getProcs()) {
			proc.execute(progress);
		}
	}

	@Override
	public String toStringAction() {
		return getDescription(getDetailedStatus());
	}

	private String getDescription(String status) {
		return String.format("%s %s, target version: %s, target branch: %s", status, comp.getCoords(), targetVersion,
				Utils.getReleaseBranchName(repo, targetVersion));
	}

	private String getDetailedStatus() {
		String skipStr = getProcs().isEmpty() && getBsFrom() != BuildStatus.DONE ? "skip " : "";
		String bsToStr = getBsTo() != null && getBsTo() != getBsFrom() ? " -> " + getBsTo() : "";
		return skipStr + getSimpleStatus() + bsToStr;
	}

	@Override
	public String toString() {
		return getDescription(getSimpleStatus());
	}

	private String getSimpleStatus() {
		return getBsFrom().toString();
	}

	public BuildStatus getBsFrom() {
		return bsFrom;
	}

	public BuildStatus getBsTo() {
		return bsTo;
	}

	public List<ISCMProc> getProcs() {
		return procs;
	}

	@Override
	public boolean isExecutable() {
		return bsFrom != BuildStatus.DONE;
	}
}
