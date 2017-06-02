package org.scm4j.wf;

import java.util.ArrayList;
import java.util.List;

import org.scm4j.actions.ActionAbstract;
import org.scm4j.actions.IAction;
import org.scm4j.actions.results.ActionResultVersion;
import org.scm4j.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.wf.conf.ConfFile;
import org.scm4j.wf.conf.MDepsFile;
import org.scm4j.wf.conf.Version;
import org.scm4j.wf.model.Dep;
import org.scm4j.wf.model.VCSRepository;

public class SCMActionProductionRelease extends ActionAbstract {
	
	public static final String VCS_TAG_SCM_VER = "#scm-ver";
	public static final String VCS_TAG_SCM_MDEPS = "#scm-mdeps";
	public static final String VCS_TAG_SCM_IGNORE = "#scm-ignore";
	public static final String VCS_TAG_SCM_RELEASE = "#scm-ver release";
	public static final String[] VCS_TAGS = new String[] {VCS_TAG_SCM_VER, VCS_TAG_SCM_MDEPS, VCS_TAG_SCM_IGNORE};
	public static final String BRANCH_DEVELOP = "develop";
	public static final String BRANCH_RELEASE = "release";
	
	private ProductionReleaseReason reason;

	public SCMActionProductionRelease(VCSRepository repo, List<IAction> childActions, String masterBranchName, 
			ProductionReleaseReason reason) {
		super(repo, childActions, masterBranchName);
		this.reason = reason;
	}

	public ProductionReleaseReason getReason() {
		return reason;
	}

	public void setReason(ProductionReleaseReason reason) {
		this.reason = reason;
	}

	@Override
	public Object execute(IProgress progress) {
		try {
			
			IVCS vcs = getVCS();
			Version currentVer = getDevVersion();
			progress.reportStatus("current trunk version: " + currentVer);
			
			/**
			 * выполним все экшены и получим результаты.
			 * Составим таблицу новых версий (которые получились в итоге)
			 */
			Object nestedResult;
			for (IAction action : childActions) {
				try (IProgress nestedProgress = progress.createNestedProgress(action.getName())) {
					nestedResult = action.execute(nestedProgress);
					if (nestedResult instanceof Throwable) {
						return nestedResult;
					}
				}
				getResults().put(action.getName(), nestedResult);
			}
			
			// А не построены ли мы уже?
			if (getResults().get(getName()) != null) {
				Object existingResult = getResults().get(getName());
				if (existingResult instanceof ActionResultVersion) {
					progress.reportStatus("using already built version " + ((ActionResultVersion) existingResult).getVersion()); 
					return existingResult;
				}
			}
			
			// тут у нас мапа с новыми версиями. Будем прописывать их в mdeps под ногами.
			VCSCommit newVersionStartsFromCommit;
			List<String> mDepsChanged = new ArrayList<>();
			if (vcs.fileExists(currentBranchName, SCMWorkflow.MDEPS_FILE_NAME)) {
				String mDepsContent = vcs.getFileContent(currentBranchName, SCMWorkflow.MDEPS_FILE_NAME);
				MDepsFile mDepsFile = new MDepsFile(mDepsContent, repo);
				List<String> mDepsOut = new ArrayList<>();
				for (Dep mDep : mDepsFile.getMDeps()) {
					String mDepName = mDep.getName();
					nestedResult = getResults().get(mDepName);
					if (nestedResult != null && nestedResult instanceof ActionResultVersion) {
						ActionResultVersion res = (ActionResultVersion) nestedResult;
						if (res.getIsNewBuild()) {
							String mDepOut = mDepName + ":" + res.getVersion();
							mDepsOut.add(mDepOut);
							mDepsChanged.add(mDepOut);
						} else {
							// тут посмотрим: если у нас в untillDb 5.0 (или вообще null), а в action.ver 7.1, то пропишем в mdeps unTillDb 7.0
							String mDepVer = mDep.getVersion().toString();
							if (!res.getVersion().equals(mDepVer)) {
								mDep.setVersion(new Version(res.getVersion()));
								String mDepOut = mDep.toString();
								mDepsOut.add(mDepOut);
								mDepsChanged.add(mDepOut);
							} else {
								mDepsOut.add(mDep.toString());
							}
						}
					} else {
						mDepsOut.add(mDep.toString());
					}
				}
				progress.reportStatus("new mdeps generated");
				
				String mDepsOutContent = ConfFile.toFileContent(mDepsOut);
				newVersionStartsFromCommit = vcs.setFileContent(currentBranchName, SCMWorkflow.MDEPS_FILE_NAME, 
						mDepsOutContent, VCS_TAG_SCM_MDEPS);
				if (newVersionStartsFromCommit == VCSCommit.EMPTY) {
					// зависимости не изменились, но для нас самих надо сделать релиз
					newVersionStartsFromCommit = vcs.getHeadCommit(currentBranchName);
					progress.reportStatus("mdeps file is not changed. Going to branch from " + newVersionStartsFromCommit);
				} else {
					progress.reportStatus("mdeps updated in trunk, revision " + newVersionStartsFromCommit);
				}
			} else {
				newVersionStartsFromCommit = vcs.getHeadCommit(currentBranchName);
				progress.reportStatus("no mdeps. Going to branch from head " + newVersionStartsFromCommit);
			}
			
			// отведем ветку
			String newBranchName = repo.getReleaseBanchPrefix() + currentVer.toReleaseString(); 
			vcs.createBranch(currentBranchName, newBranchName, "branch created");
			progress.reportStatus("branch " + newBranchName + " created");
			
			// увеличим minor ver в транке
			String verContent = currentVer.toNextMinorSnapshot();
			vcs.setFileContent(currentBranchName, SCMWorkflow.VER_FILE_NAME, 
					verContent, VCS_TAG_SCM_VER + " " + verContent);
			progress.reportStatus("change to version " + verContent + " in trunk");
			
			// сохраним ver в ветке
			verContent = currentVer.toReleaseString();
			vcs.setFileContent(newBranchName, SCMWorkflow.VER_FILE_NAME, verContent, 
					VCS_TAG_SCM_VER + " " + verContent);
			progress.reportStatus("change to version " + verContent + " in branch " + newBranchName);
			
			// запишем mdeps-changed
			if (!mDepsChanged.isEmpty()) {
				vcs.setFileContent(newBranchName, SCMWorkflow.MDEPS_CHANGED_FILE_NAME, ConfFile.toFileContent(mDepsChanged), 
						VCS_TAG_SCM_IGNORE);
				progress.reportStatus("mdeps-changed is written to branch " + newBranchName);
			}
			
			ActionResultVersion res = new ActionResultVersion(repo.getName(), currentVer.toReleaseString(), true);
			progress.reportStatus("new " + repo.getName() + " " 
					+ res.getVersion() + " is released in " + newBranchName);
			return res;
		} catch (Throwable t) {
			progress.reportStatus("execution error: " + t.toString() + ": " + t.getMessage());
			return t;
		} 
	}
	
	@Override
	public String toString() {
		return super.toString() + "; " + reason.toString();
	}
}
