package org.scm4j.wf;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scm4j.actions.ActionAbstract;
import org.scm4j.actions.IAction;
import org.scm4j.actions.results.ActionResultVersion;
import org.scm4j.progress.IProgress;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.VCSCommit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class SCMActionProductionRelease extends ActionAbstract {
	
	public SCMActionProductionRelease(VCSRepository repo, List<IAction> childActions, String masterBranchName) {
		super(repo, childActions, masterBranchName);
	}

	@Override
	public Object execute(IProgress progress) {
		progress.reportStatus(getName() + " execution started");
		try {
			
			IVCS vcs = getVCS();
			VerFile verFile = getVerFile();
			
			/**
			 * выполним все экшены и получим результаты.
			 * Составим таблицу новых версий (которые получились в итоге)
			 */
			
			Map<String, Object> results = new HashMap<>();
			for (IAction action : childActions) {
				progress.reportStatus(">>> " + action.getName());
				try (IProgress nestedProgress = progress.createNestedProgress(action.getName())) {
					Object result = action.execute(nestedProgress);
					if (result instanceof Throwable) {
						return result;
					}
					results.put(action.getName(), result);
				} finally {
					progress.reportStatus("<<< " + action.getName());
				}
			}
			
			// тут у нас мапа с новыми версиями. Будем прописывать их в mdeps под ногами.
			String mDepsContent = vcs.getFileContent(masterBranchName, SCMWorkflow.MDEPS_FILE_NAME);
			Type type = new TypeToken<List<String>>() {}.getType();
			List<String> mDeps = GsonUtils.fromJson(mDepsContent, type);
			List<String> mDepsOut = new ArrayList<>();
			for (String mDep : mDeps) {
				String mDepName = getMDepName(mDep);
				Object result = results.get(mDepName);
				if (result != null && result instanceof ActionResultVersion) {
					ActionResultVersion res = (ActionResultVersion) result;
					mDepsOut.add(mDepName + ":" + res.getVersion());
				} else {
					mDepsOut.add(mDep);
				}
			}
			progress.reportStatus("new mdeps generated");
			
			// увеличим версию
			Integer ver = verFile.getLastNumber();
			Integer nextVer = ver + 1;
			verFile.setLastNumber(nextVer);
			
			Gson gson = new GsonBuilder()
					.setPrettyPrinting()
					.create();
			String mDepsOutContent = gson.toJson(mDepsOut);
			VCSCommit newVersionStartsFromCommit = vcs.setFileContent(masterBranchName, SCMWorkflow.MDEPS_FILE_NAME, 
					mDepsOutContent, SCMWorkflow.MDEPS_FILE_NAME + " updated");
			if (newVersionStartsFromCommit == VCSCommit.EMPTY) {
				// зависимости не изменились, но для нас самих надо сделать релиз
				String newVerFileContent = gson.toJson(verFile);
				newVersionStartsFromCommit = vcs.setFileContent(masterBranchName, 
						SCMWorkflow.VER_FILE_NAME, newVerFileContent, "change version to " + verFile.getVer());
				progress.reportStatus("ver file updated in trunk, revision " + newVersionStartsFromCommit);
			} else {
				progress.reportStatus("mdeps updated in trunk, revision " + newVersionStartsFromCommit);
			}
			
			// отведем ветку
			
			String newBranchName = "B" + ver.toString() + ".1";
			vcs.createBranch(masterBranchName, newBranchName, "branch created");
			progress.reportStatus("branch " + newBranchName + " created");
			
			// сохраним lastVerCommit в транке
			verFile.setLastVerCommit(newVersionStartsFromCommit.getId());
			String verContent = gson.toJson(verFile);
			vcs.setFileContent(masterBranchName, SCMWorkflow.VER_FILE_NAME, 
					verContent, SCMWorkflow.VER_FILE_NAME + " lastVerCommit written");
			progress.reportStatus("change to version " + verFile.getVer() + ", lastVerCommit = " 
					+ verFile.getLastVerCommit() + " in trunk");
			
			// сохраним verCommit в ветке
			verFile.setLastVerCommit(null);
			verFile.setVerCommit(newVersionStartsFromCommit.getId());
			verFile.setVer(ver.toString() + ".1");
			verContent = gson.toJson(verFile);
			vcs.setFileContent(newBranchName, SCMWorkflow.VER_FILE_NAME, verContent, 
					SCMWorkflow.VER_FILE_NAME + " verCommit written");
			progress.reportStatus("verCommit written to " + newBranchName);
			
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
