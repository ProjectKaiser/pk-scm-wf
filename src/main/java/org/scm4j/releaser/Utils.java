package org.scm4j.releaser;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.TagDesc;
import org.scm4j.releaser.exceptions.EReleaserException;

public final class Utils {

	public static final File RELEASES_DIR = new File(System.getProperty("user.dir"), "releases");
	public static final String ZERO_PATCH = "0";
	public static final String VER_FILE_NAME = "version";
	public static final String MDEPS_FILE_NAME = "mdeps";
	public static final String DELAYED_TAGS_FILE_NAME = "delayed-tags.yml";
	public static final File BASE_WORKING_DIR = new File(System.getProperty("user.home"), ".scm4j");

	private static final int THREADS_AMOUNT = 10;

	public static <T> T reportDuration(Supplier<T> sup, String message, Component comp, IProgress progress) {
		if (progress == null) {
			return sup.get();
		}
		long start = System.currentTimeMillis();
		T res = sup.get();
		progress.reportStatus(String.format("%s: %s in %dms", message, comp == null ? "" : comp.getCoordsNoComment(),
				System.currentTimeMillis() - start));
		return res;
	}

	public static <T> void reportDuration(Runnable run, String message, Component comp, IProgress progress) {
		reportDuration(() -> {
			run.run();
			return null;
		}, message, comp, progress);
	}

	private Utils() {
	}

	public static <T> void async(Collection<T> collection, Consumer<? super T> action) {
		async(collection, action, new ForkJoinPool(THREADS_AMOUNT));
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
				throw new EReleaserException(task.getException());
			}
		}
	}

	public static String getReleaseBranchName(Component comp, Version forVersion) {
		return comp.getVcsRepository().getReleaseBranchPrefix() + forVersion.getReleaseNoPatchString();
	}

	public static File getBuildDir(Component comp, Version forVersion) {
		File buildDir = new File(RELEASES_DIR, comp.getUrl().replaceAll("[^a-zA-Z0-9.-]", "_"));
		buildDir = new File(buildDir, getReleaseBranchName(comp, forVersion).replaceAll("[^a-zA-Z0-9.-]", "_"));
		return buildDir;
	}

	public static TagDesc getTagDesc(String verStr) {
		String tagMessage = verStr + " release";
		return new TagDesc(verStr, tagMessage);
	}

	public static Version getDevVersion(Component comp) {
		return new Version(
				comp.getVCS().getFileContent(comp.getVcsRepository().getDevelopBranch(), Utils.VER_FILE_NAME, null));
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
}
