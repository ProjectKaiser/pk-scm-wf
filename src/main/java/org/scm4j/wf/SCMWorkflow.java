package org.scm4j.wf;

import org.scm4j.wf.actions.ActionError;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branchstatus.DevelopBranch;
import org.scm4j.wf.branchstatus.DevelopBranchStatus;
import org.scm4j.wf.branchstatus.ReleaseBranch;
import org.scm4j.wf.branchstatus.ReleaseBranchStatus;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.exceptions.EComponentConfig;
import org.scm4j.wf.exceptions.EConfig;
import org.scm4j.wf.scmactions.*;

import java.util.ArrayList;
import java.util.List;

public class SCMWorkflow implements ISCMWorkflow {

	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String MDEPS_CHANGED_FILE_NAME = "mdeps-changed";

	private final VCSRepositories repos;
	private final Component comp;
	private final DevelopBranch db;
	private final List<Component> mDeps;

	public SCMWorkflow(String coords, VCSRepositories repos) {
		this(new Component(coords, repos), repos);
	}

	public SCMWorkflow(Component comp, VCSRepositories repos) {
		this.repos = repos;
		this.comp = comp;
		db = new DevelopBranch(comp);
		mDeps = db.getMDeps();
	}
	
	public SCMWorkflow(String coords) throws EConfig {
		this(coords, VCSRepositories.loadVCSRepositories());
	}

	@Override
	public IAction getProductionReleaseAction(List<IAction> childActions) {
		if (childActions == null) {
			childActions = new ArrayList<>();
		}
		
		for (Component mDep : mDeps) {
			ISCMWorkflow childWorkflow = new SCMWorkflow(mDep, repos);
			childActions.add(childWorkflow.getProductionReleaseAction(null));
		}
		return getProductionReleaseActionRoot(childActions);
	}

	public IAction getProductionReleaseActionRoot(List<IAction> childActions) {
		if (!db.hasVersionFile()) {
			throw new EComponentConfig("no " + VER_FILE_NAME + " file for " + comp.toString());
		}
		
		if (hasErrorActions(childActions)) {
			return new ActionNone(comp, childActions, "has child error actions");
		}
		if (db.getStatus() == DevelopBranchStatus.MODIFIED) {
			ReleaseBranch rb = new ReleaseBranch(comp, repos);
			if (!rb.exists()) {
				return new SCMActionForkReleaseBranch(comp, childActions);
			}
			return new SCMActionProductionRelease(comp, childActions, ProductionReleaseReason.NEW_FEATURES);
		}
		if (hasSignificantActions(childActions) || hasNewerDependencies(childActions, mDeps)) {
			ReleaseBranch rb = new ReleaseBranch(comp, repos);
			if (!rb.exists()) {
				return new SCMActionForkReleaseBranch(comp, childActions);
			}
			return new SCMActionProductionRelease(comp, childActions, ProductionReleaseReason.NEW_DEPENDENCIES);
		}
		return new SCMActionUseLastReleaseVersion(comp, childActions);
	}

	private boolean hasNewerDependencies(List<IAction> actions, List<Component> mDeps) {
		for (IAction action : actions) {
			if (action instanceof SCMActionUseLastReleaseVersion) {
				SCMActionUseLastReleaseVersion verAction = (SCMActionUseLastReleaseVersion) action;
				for (Component comp : mDeps) {
					if (comp.getCoords().getName().equals(verAction.getName()) && (comp.getVersion() == null || !comp.getVersion()
							.toPreviousMinorRelease().equals(verAction.getVersion().toPreviousMinorRelease()))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean hasErrorActions(List<IAction> actions) {
		for (IAction action : actions) {
			if (action instanceof ActionError) {
				return true;
			}
		}
		return false;
	}

	private boolean hasSignificantActions(List<IAction> actions) {
		for (IAction action : actions) {
			if (!(action instanceof ActionNone) && !(action instanceof SCMActionUseLastReleaseVersion)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public IAction getTagReleaseAction(List<IAction> childActions) {
		if (childActions == null) {
			childActions = new ArrayList<>();
		}
		for (Component mDep : mDeps) {
			ISCMWorkflow childWorkflow = new SCMWorkflow(mDep, repos);
			childActions.add(childWorkflow.getTagReleaseAction(null));
		}
		return getTagReleaseActionRoot(childActions);
	}

	private IAction getTagReleaseActionRoot(List<IAction> childActions) {
		ReleaseBranch rb = new ReleaseBranch(comp, repos);
		if (rb.getStatus() == ReleaseBranchStatus.TAGGED) {
			return new SCMActionUseExistingTag(comp, childActions, rb.getReleaseTag());
		} else {
			return new SCMActionTagRelease(comp, childActions, "tag message");
		}
	}
}
