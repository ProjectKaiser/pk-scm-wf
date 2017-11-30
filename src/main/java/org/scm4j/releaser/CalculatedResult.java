package org.scm4j.releaser;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.branch.WorkingBranch;
import org.scm4j.releaser.conf.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CalculatedResult {

	private final Map<String, BuildStatus> buildStatuses = new HashMap<>();
	private final Map<String, List<Component>> mDeps = new HashMap<>();
	private final Map<String, Boolean> needesToFork = new HashMap<>();
	private final Map<String, WorkingBranch> releaseBranches = new HashMap<>();
	
	private <T> T get(Component comp, Map<String, T> cache) {
		return cache.get(getKey(comp));
	}
	
	private <T> T getOrSet(Component comp, Map<String, T> cache, Supplier<T> sup) {
		T res = get(comp, cache);
		if (res == null) {
			res = sup.get();
			cache.put(getKey(comp), res);
		}
		return res;
	}
	
	public synchronized BuildStatus getBuildStatus(Component comp) {
		return get(comp, buildStatuses);
	}
	
	public synchronized BuildStatus setBuildStatus(Component comp, Supplier<BuildStatus> sup, IProgress progress) {
		return Utils.reportDuration(() -> getOrSet(comp, buildStatuses, sup), "build status", comp, progress);
	}
	
	public synchronized List<Component> getMDeps(Component comp) {
		return get(comp, mDeps);
	}
	
	public synchronized List<Component> setMDeps(Component comp, Supplier<List<Component>> sup, IProgress progress) {
		return Utils.reportDuration(() -> getOrSet(comp, mDeps, sup), "mdeps retrieve", comp, progress);
	}

	public synchronized List<Component> setMDeps(Component comp, Supplier<List<Component>> sup) {
		return setMDeps(comp, sup, null);
	}
	
	public synchronized Boolean setNeedsToFork(Component comp, Supplier<Boolean> sup, IProgress progress) {
		return Utils.reportDuration(() -> getOrSet(comp, needesToFork, sup), "need to fork", comp, progress);
	}

	public synchronized Boolean setNeedsToFork(Component comp, Supplier<Boolean> sup) {
		return setNeedsToFork(comp, sup, null);
	}
	
	public synchronized WorkingBranch getReleaseBranch(Component comp) {
		return get(comp, releaseBranches);
	}
	
	public synchronized WorkingBranch setReleaseBranch(Component comp, Supplier<WorkingBranch> sup, IProgress progress) {
		return Utils.reportDuration(() -> getOrSet(comp, releaseBranches, sup), "release branch", comp, progress);
	}

	public WorkingBranch setReleaseBranch(Component comp,  Supplier<WorkingBranch> sup) {
		return setReleaseBranch(comp, sup, null);
	}

	public synchronized void replaceReleaseBranch(Component comp, WorkingBranch rb) {
		releaseBranches.replace(getKey(comp), rb);
	}

	private String getKey(Component comp) {
		return comp.getVcsRepository().getUrl();
	}
}
