package org.scm4j.releaser.scmactions;

import java.util.ArrayList;
import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatusTreeNode;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.scmactions.procs.ISCMProc;
import org.scm4j.releaser.scmactions.procs.SCMProcActualizePatches;
import org.scm4j.releaser.scmactions.procs.SCMProcBuild;
import org.scm4j.releaser.scmactions.procs.SCMProcForkBranch;
import org.scm4j.releaser.scmactions.procs.SCMProcFreezeMDeps;

public class SCMActionRelease extends ActionAbstract {

	private final List<ISCMProc> procs = new ArrayList<>();
	private final BuildStatus bsFrom;
	private final BuildStatus bsTo;
	private final Version targetVersion;
	
	public SCMActionRelease(Component comp, List<IAction> childActions, CachedStatuses cache, ActionSet actionSet) {
		super(comp, childActions);
		ExtendedStatusTreeNode status = cache.get(comp.getUrl());
		this.bsFrom = status.getStatus();
		targetVersion = status.getLatestVersion();
		
		BuildStatus bsTo = null;
		switch(bsFrom) {
		case FORK:
			procs.add(new SCMProcForkBranch(comp, cache));
		case FREEZE:
			getProcs().add(new SCMProcFreezeMDeps(comp, cache));
			bsTo = BuildStatus.FREEZE;
			if (actionSet == ActionSet.FORK_ONLY) {
				break;
			}
		case BUILD_MDEPS:
		case ACTUALIZE_PATCHES:
			if (bsFrom.ordinal() > BuildStatus.FREEZE.ordinal() && actionSet == ActionSet.FULL) {
				getProcs().add(new SCMProcActualizePatches(comp, cache));
			}
		case BUILD:
			if (actionSet == ActionSet.FULL) {
				getProcs().add(new SCMProcBuild(comp, cache));
				bsTo = BuildStatus.BUILD;
			}
			break;
		case DONE: 
			break;
		default:
			throw new IllegalArgumentException("unsupported build status: " + bsFrom);
		}
		this.bsTo = bsTo;
	}

//	public SCMActionRelease(ReleaseBranch rb, Component comp, List<IAction> childActions, ActionSet actionSet, BuildStatus bs, CalculatedResult calculatedResult) {
//		super(comp, childActions);
//		this.bsFrom = bs;
//		BuildStatus bsTo = null;
//		if (bs.ordinal() > BuildStatus.FREEZE.ordinal()) {
//			targetRB = rb;
//		} else {
//			targetRB = new ReleaseBranch(comp, rb.getVersion().toNextMinor().toReleaseZeroPatch());
//		}
//		switch (bs) {
//		case FORK:
//			getProcs().add(new SCMProcForkBranch(targetRB, comp, calculatedResult));
//		case FREEZE:
//			getProcs().add(new SCMProcFreezeMDeps(targetRB, comp, calculatedResult));
//			bsTo = BuildStatus.FREEZE;
//			if (actionSet == ActionSet.FORK_ONLY) {
//				break;
//			}
//		case BUILD_MDEPS:
//		case ACTUALIZE_PATCHES:
//			if (bs.ordinal() > BuildStatus.FREEZE.ordinal() && actionSet == ActionSet.FULL) {
//				getProcs().add(new SCMProcActualizePatches(targetRB, comp, calculatedResult));
//			}
//		case BUILD:
//			if (actionSet == ActionSet.FULL) {
//				getProcs().add(new SCMProcBuild(targetRB, comp, calculatedResult));
//				bsTo = BuildStatus.BUILD;
//			}
//			break;
//		case DONE: 
//			break;
//		default:
//			throw new IllegalArgumentException("unsupported build status: " + bs);
//		}
//		this.bsTo = bsTo;
//	}

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
		return String.format("%s %s, target version: %s, target branch: %s", status, comp.getCoords(), targetVersion, Utils.getReleaseBranchName(comp, targetVersion));
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
}
