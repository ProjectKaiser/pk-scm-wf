package org.scm4j.releaser.actions;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.BuildStatus;
import org.scm4j.releaser.branch.ReleaseBranch;

import java.util.List;

public class ActionNone extends ActionAbstract {
	
	private final BuildStatus mbs;
	private final String reason;
	
	public ActionNone(ReleaseBranch rb, List<IAction> childActions, BuildStatus mbs) {
		super(rb.getComponent(), childActions);
		this.mbs = mbs;
		reason = null;
	}

	public ActionNone(ReleaseBranch rb, List<IAction> childActions, BuildStatus mbs, String reason) {
		super(rb.getComponent(), childActions);
		this.mbs = mbs;
		this.reason = reason;
	}

	@Override
	public void execute(IProgress progress) {
	}
	
	@Override
	public String toString() {
		return "none " + comp.getCoords().toString() + (mbs == null ? "" : ", " + mbs) + (reason == null ? "" : ", " + reason);
	}

	public BuildStatus getMbs() {
		return mbs;
	}

	public String getReason() {
		return reason;
	}
}
