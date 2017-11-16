package org.scm4j.releaser;

import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.conf.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CalculatedResult {

	private final Map<String, BuildStatus> buildStatuses = new HashMap<>();
	private final Map<String, List<Component>> mDeps = new HashMap<>();
	private final Map<String, Boolean> needesToFork = new HashMap<>();
	private final Map<String, ReleaseBranch> releaseBranches = new HashMap<>();
	
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
	
	public synchronized BuildStatus setBuildStatus(Component comp, Supplier<BuildStatus> sup) {
		return getOrSet(comp, buildStatuses, sup);
	}
	
	public synchronized List<Component> getMDeps(Component comp) {
		return get(comp, mDeps);
	}
	
	public synchronized List<Component> setMDeps(Component comp, Supplier<List<Component>> sup) {
		return getOrSet(comp, mDeps, sup);
	}
	
	public synchronized Boolean setNeedsToFork(Component comp, Supplier<Boolean> sup) {
		return getOrSet(comp, needesToFork, sup);
	}
	
	public synchronized ReleaseBranch getReleaseBranch(Component comp) {
		return get(comp, releaseBranches);
	}
	
	public synchronized ReleaseBranch setReleaseBranch(Component comp, Supplier<ReleaseBranch> sup) {
		return getOrSet(comp, releaseBranches, sup);
	}

	public synchronized void replaceReleaseBranch(Component comp, ReleaseBranch rb) {
		releaseBranches.replace(getKey(comp), rb);
	}

	private String getKey(Component comp) {
		return comp.getVcsRepository().getUrl();
	}
}
