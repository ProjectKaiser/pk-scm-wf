package org.scm4j.releaser.scmactions;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.actions.ActionAbstract;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.scmactions.procs.*;

import java.util.ArrayList;
import java.util.List;

public class SCMActionRelease extends ActionAbstract {

	private final List<ISCMProc> procs = new ArrayList<>();
	private final BuildStatus bsFrom;
	private final BuildStatus bsTo;
	private final ReleaseBranch targetRB;

	public SCMActionRelease(ReleaseBranch rb, List<IAction> childActions, ActionKind actionSet, BuildStatus bs) {
		super(rb.getComponent(), childActions);
		this.bsFrom = bs;
		BuildStatus bsTo = null;
		if (bs.ordinal() > BuildStatus.FREEZE.ordinal()) {
			targetRB = rb;
		} else {
			targetRB = new ReleaseBranch(comp, rb.getVersion().toNextMinor().toReleaseZeroPatch());
		}
		switch (bs) {
		case FORK:
			getProcs().add(new SCMProcForkBranch(targetRB));
		case FREEZE:
			getProcs().add(new SCMProcFreezeMDeps(targetRB));
			bsTo = BuildStatus.FREEZE;
			if (actionSet == ActionKind.FORK_ONLY) {
				break;
			}
		case BUILD_MDEPS:
		case ACTUALIZE_PATCHES:
			if (bs.ordinal() > BuildStatus.FREEZE.ordinal() && actionSet == ActionKind.FULL) {
				getProcs().add(new SCMProcActualizePatches(targetRB));
			}
		case BUILD:
			if (actionSet == ActionKind.FULL) {
				getProcs().add(new SCMProcBuild(targetRB));
				bsTo = BuildStatus.BUILD;
				
			}
			break;
		case DONE: 
			break;
		default:
			throw new IllegalArgumentException("unsupported build status: " + bs);
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
		String skipStr = getProcs().isEmpty() && getBsFrom() != BuildStatus.DONE ? "skip " : ""; 
		String bsToStr = getBsTo() != null && getBsTo() != getBsFrom() ? " -> " + getBsTo() : "";
		return String.format("%s%s%s %s, target version %s, target branch: %s", skipStr, getBsFrom(), bsToStr , comp.getCoords(), targetRB.getVersion(), targetRB.getName());
	}

	@Override
	public String toString() {
		return String.format("%s %s, target version: %s", getBsFrom(), comp.getCoords(), targetRB.getVersion());
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
