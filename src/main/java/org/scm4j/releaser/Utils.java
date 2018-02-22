package org.scm4j.releaser;

import org.apache.commons.io.FileUtils;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.conf.VCSRepository;
import org.scm4j.releaser.conf.VCSType;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.vcs.api.workingcopy.IVCSRepositoryWorkspace;
import org.scm4j.vcs.api.workingcopy.IVCSWorkspace;
import org.scm4j.vcs.api.workingcopy.VCSWorkspace;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;
import java.util.function.Supplier;


public final class Utils {

	public static final File RELEASES_DIR = new File(System.getProperty("user.dir"), "releases");
	public static final String ZERO_PATCH = "0";
	public static final String VER_FILE_NAME = "version";
	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final File BASE_WORKING_DIR = new File(System.getProperty("user.home"), ".scm4j");

	public static <T> T reportDuration(Supplier<T> sup, String message, Component comp, IProgress progress) {
		if (progress == null) {
			return sup.get();
		}
		long start = System.currentTimeMillis();
		T res = sup.get();
		progress.reportStatus(String.format("%s:%s in %dms", message, comp == null ? "" : " " + comp.getCoordsNoComment(),
				System.currentTimeMillis() - start));
		return res;
	}

	public static <T> void reportDuration(Runnable run, String message, Component comp, IProgress progress) {
		reportDuration(() -> {
			run.run();
			return null;
		}, message, comp, progress);
	}

	public static <T> void async(Collection<T> collection, Consumer<? super T> action) {
		async(collection, action, ForkJoinPool.commonPool());
	}

	public static <T> void async(Collection<T> collection, Consumer<? super T> action, ForkJoinPool pool) {
		if (collection.isEmpty()) {
			return;
		}
		// http://jsr166-concurrency.10961.n7.nabble.com/ForkJoinPool-not-designed-for-nested-Java-8-streams-parallel-forEach-td10977.html
		if (pool.getParallelism() == 1) {
			for (T element : collection) {
				action.accept(element);
			}
		} else {
			ForkJoinTask<?> task = pool.submit(() -> {
				collection.parallelStream().forEach(action);
			});
			task.invoke();
			if (task.getException() != null) {
				throw new EReleaserException((Exception) task.getException());
			}
		}
	}

	public static String getReleaseBranchName(VCSRepository repo, Version forVersion) {
		return repo.getReleaseBranchPrefix() + forVersion.getReleaseNoPatchString();
	}

	public static File getBuildDir(VCSRepository repo, Version forVersion) {
		IVCSWorkspace ws = new VCSWorkspace(RELEASES_DIR.toString());
		IVCSRepositoryWorkspace rws = ws.getVCSRepositoryWorkspace(repo.getUrl());
		return new File(rws.getRepoFolder(), getReleaseBranchName(repo, forVersion).replaceAll("[^a-zA-Z0-9.-]", "_"));
	}

	public static TagDesc getTagDesc(String verStr) {
		String tagMessage = verStr + " release";
		return new TagDesc(verStr, tagMessage);
	}

	public static Version getDevVersion(VCSRepository repo) {
		return new Version(
				repo.getVCS().getFileContent(repo.getDevelopBranch(), Utils.VER_FILE_NAME, null));
	}

	public static void waitForDeleteDir(File dir) throws Exception {
		for (Integer i = 1; i <= 10; i++) {
			try {
				FileUtils.deleteDirectory(dir);
				break;
			} catch (Exception e) {
				Thread.sleep(100);
			}
		}
		if (dir.exists()) {
			throw new Exception("failed to delete " + dir);
		}
	}

	public static Map<String, String> getBuildTimeEnvVars(VCSType vcsType, String buildRevision, String releaseBranchName, String url) {
		String envVarRevision = null;
		String envVarBranch = null;
		String envVarUrl = null;
		switch(vcsType) {
			case GIT:
				envVarRevision = "GIT_COMMIT";
				envVarBranch = "GIT_BRANCH";
				envVarUrl = "GIT_URL";
				break;
			case SVN:
				envVarRevision = "SVN_REVISION";
				envVarBranch = "SVN_BRANCH";
				envVarUrl = "SVN_URL";
				break;
		}
		Map<String, String> res = new HashMap<>();
		res.put(envVarRevision, buildRevision);
		res.put(envVarBranch, releaseBranchName);
		res.put(envVarUrl, url);
		return res;
	}
}
