import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scm4j.releaser.Utils;

public class AsyncTest {
	
	private ForkJoinPool poolForRejectedExecution = new ForkJoinPool(2);
	private ForkJoinPool poolForInsufficientThreads = new ForkJoinPool(2);
	private int counter = 0;
	private List<Integer> list1 = Arrays.asList(1, 2, 3, 4, 5);
	private List<Integer> list2 = Arrays.asList(6, 7, 8, 9, 10);
	private List<Integer> list3 = Arrays.asList(11, 12, 13, 14, 15);
	private long startMs;
	
	@Before
	public void setUp() {
		startMs = System.currentTimeMillis();
	}
	
	@After
	public void tearDown() {
		System.out.println("done in: " + (System.currentTimeMillis() - startMs));
	}
	
	@Test
	public void rejectedExecution() {
		// RejectedExecution is thrown on submit after shutdown()
		Utils.async(list1, (x) -> {
			System.out.println(x);
			poolForRejectedExecution.shutdown();
			rejectedExecution();
		}, poolForRejectedExecution);
	}
	
	@Test
	public void insufficientThreads() {
		// not enough threads to execute recursive submits
//		Utils.async(list1, (x) -> {
//	    	System.out.println(x);
//			insufficientThreads();
//	    }, poolForInsufficientThreads);
		ForkJoinTask<?> task = poolForInsufficientThreads.submit(() -> {
	    	try {
	    		counter++;
	    		if (counter < 5) {
	    			insufficientThreads();
	    		}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	    });
	    while (!task.isDone()) {
	    	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}
	
	@Test
	public void insufficientThreadsButStealing() throws Exception {
		// not enough threads to execute recursive submits
		long start = System.currentTimeMillis();
	    ForkJoinTask<?> task = poolForInsufficientThreads.submit(() -> {
	    	try {
	    		System.out.println(poolForInsufficientThreads.getActiveThreadCount());
	    		ForkJoinTask<?> task2 = poolForInsufficientThreads.submit(() -> {
	    	    	try {
	    	    		System.out.println(poolForInsufficientThreads.getActiveThreadCount());
	    	    		ForkJoinTask<?> task3 = poolForInsufficientThreads.submit(() -> {
	    	    	    	try {
	    	    	    		System.out.println(poolForInsufficientThreads.getActiveThreadCount());
	    	    			} catch (Exception e) {
	    	    				throw new RuntimeException(e);
	    	    			}
	    	    	    });
	    	    		task3.get();
	    			} catch (Exception e) {
	    				throw new RuntimeException(e);
	    			}
	    	    });
	    		task2.get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	    });
	    task.get();
	    System.out.println("done in: " + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testAsync() throws Exception {
		Set<String> thNames = new HashSet<>();
		ForkJoinPool pool = new ForkJoinPool(1);
		long start = System.currentTimeMillis();
	    pool.submit(() -> {
	            IntStream.range(1, 10).parallel().forEach(n -> {
	                //System.out.print("Processing n: " + n);
	                try {
	                	 pool.submit(() -> {IntStream.range(5, 10).parallel().forEach(n1 -> {
	    	                //System.out.print("Processing n: " + n1);
	    	                try {
	    	                	pool.submit(() -> {IntStream.range(10, 15).parallel().forEach(n2 -> {

	    	    	                System.out.print("Processing n: " + n2);
	    	    	                try {
	    	    	                    //Thread.sleep(500);
	    	    	                    thNames.add(Thread.currentThread().getName());
	    	    	                    System.out.println("finished processing n: " + n2);
	    	    	                    //System.out.println("Size: " + thNames.size() + ", activeCount: " + pool.getActiveThreadCount());
	    	    	                } catch (Exception e) {
	    	    	                    throw new RuntimeException(e);
	    	    	                }
	    	                	 });}).get();
	    	                    //Thread.sleep(500);
	    	                   // System.out.println("finished processing n: " + n1);
	    	                    thNames.add(Thread.currentThread().getName());
	    	                    //System.out.println("Size: " + thNames.size() + ", activeCount: " + pool.getActiveThreadCount());
	    	                } catch (Exception e) {
	    	                    throw new RuntimeException(e);
	    	                }
	                	 });}).get();
	                    Thread.sleep(500);
	                    //System.out.println("finished processing n: " + n);
	                    thNames.add(Thread.currentThread().getName());
	                    //System.out.println("Size: " + thNames.size() + ", activeCount: " + pool.getActiveThreadCount());
	                } catch (Exception e) {
	                    throw new RuntimeException(e);
	                }
	            });
	        }).get();
	    System.out.println("done in: " + (System.currentTimeMillis() - start));
		
	}

}
