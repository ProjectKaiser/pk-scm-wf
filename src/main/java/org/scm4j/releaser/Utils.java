package org.scm4j.releaser;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.conf.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Utils {

	public static <T> T reportDuration(Supplier<T> sup, String message, Component comp, IProgress progress) {
		if (progress == null) {
			return sup.get();
		}
		long start = System.currentTimeMillis();
		T res = sup.get();
		progress.reportStatus(message + ": " + (comp == null ? "" : comp.getCoordsNoComment() + " ") + "in " + (System.currentTimeMillis() - start) + "ms");
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

	public static <T> void async(Collection<T> collection, Consumer<? super T> action) throws Exception {
		ForkJoinPool pool = new ForkJoinPool(8);
		pool.submit(() -> collection.parallelStream().forEach(action)).get();
		pool.shutdown();
	}
}
