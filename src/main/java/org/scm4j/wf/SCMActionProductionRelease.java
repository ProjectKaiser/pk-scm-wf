package org.scm4j.wf;

import java.util.ArrayList;
import java.util.List;

import org.scm4j.actions.ActionAbstract;
import org.scm4j.actions.IAction;
import org.scm4j.actions.results.ActionResultVersion;
import org.scm4j.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

public class SCMActionProductionRelease extends ActionAbstract {
	
	public SCMActionProductionRelease(VCSRepository repo, List<IAction> childActions, String masterBranchName) {
		super(repo, childActions, masterBranchName);
	}

	@Override
	public Object execute(IProgress progress) {
		progress.reportStatus(getName() + " execution started");
		Object result;
		Object nestedResult;
		try {
			
			IVCS vcs = getVCS();
			VerFile verFile = getVerFile();
			progress.reportStatus("current trunk version: " + verFile);
			
			/**
			 * выполним все экшены и получим результаты.
			 * Составим таблицу новых версий (которые получились в итоге)
			 */
			
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
			
			// увеличим версию
			Integer ver = verFile.getLastNumber();
			Integer nextVer = ver + 1;
			
			// тут у нас мапа с новыми версиями. Будем прописывать их в mdeps под ногами.
			VCSCommit newVersionStartsFromCommit;
			try {
				String mDepsContent = vcs.getFileContent(masterBranchName, SCMWorkflow.MDEPS_FILE_NAME);
				List<String> mDeps = MDepsFile.fromFileContent(mDepsContent);
				List<String> mDepsOut = new ArrayList<>();
				for (String mDep : mDeps) {
					String mDepName = getMDepName(mDep);
					nestedResult = getResults().get(mDepName);
					if (nestedResult != null && nestedResult instanceof ActionResultVersion) {
						ActionResultVersion res = (ActionResultVersion) nestedResult;
						mDepsOut.add(mDepName + ":" + res.getVersion());
					} else {
						mDepsOut.add(mDep);
					}
				}
				progress.reportStatus("new mdeps generated");
				
				String mDepsOutContent = MDepsFile.toFileContent(mDepsOut);
				newVersionStartsFromCommit = vcs.setFileContent(masterBranchName, SCMWorkflow.MDEPS_FILE_NAME, 
						mDepsOutContent, SCMWorkflow.MDEPS_FILE_NAME + " updated");
				if (newVersionStartsFromCommit == VCSCommit.EMPTY) {
					// зависимости не изменились, но для нас самих надо сделать релиз
					newVersionStartsFromCommit = vcs.getHeadCommit(masterBranchName);
					progress.reportStatus("mdeps file is not changed. Going to branch from " + newVersionStartsFromCommit);
				} else {
					progress.reportStatus("mdeps updated in trunk, revision " + newVersionStartsFromCommit);
				}
			} catch (EVCSFileNotFound e) {
				newVersionStartsFromCommit = vcs.getHeadCommit(masterBranchName);
				progress.reportStatus("no mdeps. Going to branch from " + newVersionStartsFromCommit);
			}
			
			// отведем ветку
			String newBranchName = verFile.getReleaseBranchPrefix() + ver.toString() + ".1";
			vcs.createBranch(masterBranchName, newBranchName, "branch created");
			progress.reportStatus("branch " + newBranchName + " created");
			
			// сохраним lastVerCommit и ver в транке
			verFile.setLastNumber(nextVer);
			verFile.setLastVerCommit(newVersionStartsFromCommit.getId());
			String verContent = verFile.toFileContent();
			vcs.setFileContent(masterBranchName, SCMWorkflow.VER_FILE_NAME, 
					verContent, SCMWorkflow.VER_FILE_NAME + " lastVerCommit written");
			progress.reportStatus("change to version " + verFile.getVer() + ", lastVerCommit = " 
					+ verFile.getLastVerCommit() + " in trunk");
			
			// сохраним verCommit в ветке
			verFile.setLastVerCommit(null);
			verFile.setVerCommit(newVersionStartsFromCommit.getId());
			verFile.setVer(ver.toString() + ".1");
			verContent = verFile.toFileContent();
			vcs.setFileContent(newBranchName, SCMWorkflow.VER_FILE_NAME, verContent, 
					SCMWorkflow.VER_FILE_NAME + " verCommit written");
			progress.reportStatus("verCommit " + verFile.getVerCommit() + " written to " + newBranchName);
			
			ActionResultVersion res = new ActionResultVersion();
			res.setName(repo.getName());
			res.setVersion(verFile.getVer());
			result = res;
			progress.reportStatus("new " + repo.getName() + " " 
					+ res.getVersion() + " is released in " + newBranchName);
		} catch (Throwable t) {
			result = t;
			progress.reportStatus("execution error: " + t.getMessage());
		}
		progress.reportStatus(getName() + " execution finished");
		return result;
	}

	private String getMDepName(String mDep) {
		String[] parts = mDep.split(":");
		if (parts.length < 3) {
			throw new RuntimeException("wrong coords: " + mDep);
		}
		return parts[0] + ":" + parts[1];
	}
}
