package org.scm4j.wf;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.scm4j.wf.actions.ActionError;
import org.scm4j.wf.actions.ActionNone;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.branchstatus.DevelopBranch;
import org.scm4j.wf.branchstatus.DevelopBranchStatus;
import org.scm4j.wf.branchstatus.ReleaseBranch;
import org.scm4j.wf.branchstatus.ReleaseBranchStatus;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.conf.VCSRepositories;
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.exceptions.EComponentConfig;
import org.scm4j.wf.scmactions.ProductionReleaseReason;
import org.scm4j.wf.scmactions.SCMActionForkReleaseBranch;
import org.scm4j.wf.scmactions.SCMActionBuild;
import org.scm4j.wf.scmactions.SCMActionTagRelease;
import org.scm4j.wf.scmactions.SCMActionUseExistingTag;
import org.scm4j.wf.scmactions.SCMActionUseLastReleaseVersion;

public class SCMWorkflow implements ISCMWorkflow {

	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String VER_FILE_NAME = "version";
	public static final String MDEPS_CHANGED_FILE_NAME = "mdeps-changed";

	private final VCSRepositories repos;
	
	public SCMWorkflow(VCSRepositories repos) {
		this.repos = repos;
	}
	
	public SCMWorkflow() {
		this(VCSRepositories.loadVCSRepositories());
	}
	
	@Override 
	public IAction getProductionReleaseAction(String componentName) {
		return getProductionReleaseAction(new Component(componentName, repos));
	}
	
	public IAction getProductionReleaseAction(Component comp) {
		List<IAction> childActions = new ArrayList<>();
		DevelopBranch db = new DevelopBranch(comp);
		List<Component> mDeps = db.getMDeps();
		
		for (Component mDep : mDeps) {
			childActions.add(getProductionReleaseAction(mDep));
		}
		
		return getProductionReleaseActionRoot(comp, childActions, mDeps);
	}
	
	public ReleaseBranch getReleaseBranch(Component comp) {
		DevelopBranch db = new DevelopBranch(comp);
		Version ver = db.getVersion();
		
		ReleaseBranch rb = new ReleaseBranch(comp, new Version(ver.toPreviousMinorRelease()), repos);
		ReleaseBranch prevRb = rb;
		for (int i = 0; i <= 1; i++) {
			ReleaseBranchStatus rbs = rb.getStatus();
			/**
			 * ��� ������ ������ BRANCHED? ��� ������, ��� �� ������ ������ �����, ������ ������ �� ������ ��� ��� mDeps. ����� ��� ����  
			 */
			
			if (rbs == ReleaseBranchStatus.BUILT || rbs == ReleaseBranchStatus.TAGGED) {
				return prevRb;
			}
			
			// ���� ��� ������ ����� ����� ��� �������� � develop, �� ���������� ��, ����� �� �������� RB ��� ������ 
			if (rbs != ReleaseBranchStatus.MISSING && db.getStatus() != DevelopBranchStatus.MODIFIED) {
				return rb;
			}
			prevRb = rb;
			rb = new ReleaseBranch(comp, new Version(ver.toPreviousMinorRelease()), repos);
		}
		return new ReleaseBranch(comp, repos);
	}
	
	public IAction getProductionReleaseActionRoot(Component comp, List<IAction> childActions, List<Component> mDeps) {
		DevelopBranch db = new DevelopBranch(comp);
		if (!db.hasVersionFile()) {
			throw new EComponentConfig("no " + VER_FILE_NAME + " file for " + comp.toString());
		}
		
		if (hasErrorActions(childActions)) {
			return new ActionNone(comp, childActions, "has child error actions  ");
		}
		
		// ���������, ��� �� ������ � ���
		ReleaseBranch rb = getReleaseBranch(comp);
		DevelopBranchStatus dbs = db.getStatus();
		if (dbs == DevelopBranchStatus.MODIFIED) {
			/**
			 * ����� ������� ���: ���� �� MODIFIED, �� ������ �� ��-������ �� forked. ����� ���������.
			 */
			skipAllBuilds(childActions);
			return new SCMActionForkReleaseBranch(comp, childActions);
		}
		
		// ��� ���� BRANCHED, �� ��-������ forked. � ���� IGNORED, �� ��-������ �� forked
		
		if (dbs == DevelopBranchStatus.BRANCHED) {
			/**
			 * ��� ������ �� ��-������ forked. ����� ������������ ��������� ������ ��� ��������, ���� ��� �� ���������
			 */
			ReleaseBranchStatus rbs = rb.getStatus();
			if (rbs == ReleaseBranchStatus.TAGGED || rbs == ReleaseBranchStatus.BUILT) {
				return getActionIfNewDependencies(comp, childActions, mDeps, rb);
			}
			return new SCMActionBuild(comp, childActions, ProductionReleaseReason.NEW_FEATURES, rb.getVersion());
			
//			ReleaseBranchStatus rbs = rb.getStatus();
//			if (rbs != ReleaseBranchStatus.BUILT && rbs !=ReleaseBranchStatus.TAGGED) {
//				if (rbs != ReleaseBranchStatus.BRANCHED) {
//					skipAllBuilds(childActions);
//					return new SCMActionForkReleaseBranch(comp, childActions);
//				}
//				return new SCMActionBuild(comp, childActions, ProductionReleaseReason.NEW_FEATURES, rb.getVersion());
//			}
		}
		
		// � ��� ������ ���. ���������, �� ���� �� ��� ������������� ��-�� ����� ������������
		//ReleaseBranch prevRB = new ReleaseBranch(comp, new Version(db.getVersion().toPreviousMinorRelease()), repos);
//		if (!prevRB.exists()) {
//			return new SCMActionBuild(comp, childActions, ProductionReleaseReason.NEW_DEPENDENCIES, rb.getVersion());
//		}
		
		IAction res = getActionIfNewDependencies(comp, childActions, mDeps, rb); 
		
		return res; 
//		if (hasSignificantActions(childActions)) {
//			ReleaseBranchStatus rbs = rb.getStatus();
//			if (rbs != ReleaseBranchStatus.BRANCHED) {
//				skipAllBuilds(childActions);
//				return new SCMActionForkReleaseBranch(comp, childActions);
//			}
//			return new SCMActionBuild(comp, childActions, ProductionReleaseReason.NEW_DEPENDENCIES, rb.getVersion());
//		}
		
		//return new SCMActionUseLastReleaseVersion(comp, childActions);
	}

	private IAction getActionIfNewDependencies(Component comp, List<IAction> childActions, List<Component> mDeps,
			ReleaseBranch rb) {
		DevelopBranch db = new DevelopBranch(comp);
		DevelopBranchStatus dbs = db.getStatus();
		ReleaseBranch prevRB = new ReleaseBranch(comp, new Version(db.getVersion().toPreviousMinorRelease()), repos);
		if (!mDeps.isEmpty()) {
			List<Component> mDepsFromPrevRB = prevRB.getMDeps();
			// ���� � ���������� ������ mDeps ������ �� ���� (��� ������ ����������� ������ ���) - ������ ����� ��������
			if (mDepsFromPrevRB.isEmpty() && hasSignificantActions(childActions)) { // ���� ��� testSkipBuildsIfParentUnforked
				
				
				// !!! ��� ���. ��� ���� ��-������ ��������� ����� ����� NEW_DEPENDENCIES. � �� ��������� ��� � UBL ������ ���������� �������� untilldb
				
				
				ReleaseBranchStatus rbs = rb.getStatus();
				if (rbs == ReleaseBranchStatus.MISSING) {
					skipAllBuilds(childActions);
					return new SCMActionForkReleaseBranch(comp, childActions);
				}
				return new SCMActionBuild(comp, childActions, ProductionReleaseReason.NEW_DEPENDENCIES, rb.getVersion());
			}
			for (Component curMDep : mDeps) {
				for (Component prevMDep : mDepsFromPrevRB) {
					if (prevMDep.getName().equals(curMDep.getName())) {
						if (!prevMDep.getVersion().equals(curMDep.getVersion())) {
							// ��� ��� ��� UBL. �� ��� ���� �� forked, ���� forked �� ���������� ����.
							ReleaseBranchStatus rbs = rb.getStatus();
							if (rbs == ReleaseBranchStatus.MISSING) {
								skipAllBuilds(childActions);
								return new SCMActionForkReleaseBranch(comp, childActions);
							}
							return new SCMActionBuild(comp, childActions, ProductionReleaseReason.NEW_DEPENDENCIES, rb.getVersion());
						}
					}
				}
			}
		}
		return new SCMActionUseLastReleaseVersion(comp, childActions);
	}

	private void skipAllBuilds(List<IAction> childActions) {		
		ListIterator<IAction> li = childActions.listIterator();
		IAction action;
		while (li.hasNext()) {
			action = li.next();
			if (action instanceof SCMActionBuild) {
				li.set(new ActionNone(((SCMActionBuild) action).getComponent(), action.getChildActions(), "build skipped because not all parent components forked"));
			}
		}
	}

//	private boolean hasForkActions(List<IAction> childActions) {
//		for (IAction childAction : childActions) {
//			if (childAction instanceof SCMActionForkReleaseBranch) {
//				return true;
//			}
//		}
//		return false;
//	}

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
	public IAction getTagReleaseAction(Component comp) {
		List<IAction> childActions = new ArrayList<>();
		DevelopBranch db = new DevelopBranch(comp);
		List<Component> mDeps = db.getMDeps();
		
		for (Component mDep : mDeps) {
			childActions.add(getTagReleaseAction(mDep));
		}
		return getTagReleaseActionRoot(comp, childActions);
	}

	private IAction getTagReleaseActionRoot(Component comp, List<IAction> childActions) {
		ReleaseBranch rb = new ReleaseBranch(comp, repos);
		if (rb.getStatus() == ReleaseBranchStatus.TAGGED) {
			return new SCMActionUseExistingTag(comp, childActions, rb.getReleaseTag());
		} else {
			return new SCMActionTagRelease(comp, childActions, "tag message");
		}
	}
}
