import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

import org.junit.Test;

public class AsyncTest {
	
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
