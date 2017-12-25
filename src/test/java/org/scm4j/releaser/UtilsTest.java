package org.scm4j.releaser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

import org.junit.Test;
import org.scm4j.releaser.exceptions.EReleaserException;

public class UtilsTest {
	
	@Test
	public void testAsyncParallelism() {
		ForkJoinPool pool = new ForkJoinPool(1);
		Utils.async(Collections.nCopies(3, 1), (x) -> {
			assertFalse(Thread.currentThread() instanceof ForkJoinWorkerThread);
			System.out.println(x);
		}, pool);
		
		pool = new ForkJoinPool(2);
		Utils.async(Collections.nCopies(3, 1), (x) -> {
			assertTrue(Thread.currentThread() instanceof ForkJoinWorkerThread);
			System.out.println(x);
		}, pool);
	}
	
	@Test
	public void testAsyncExceptions() {
		ForkJoinPool pool = new ForkJoinPool(2);
		RuntimeException e = new RuntimeException("test exception");
		try {
			Utils.async(Collections.nCopies(3, 1), (x) -> {
				throw e;
			}, pool);
		} catch (EReleaserException e1) {
			assertEquals(e, e1.getCause());
		}
	}
}
