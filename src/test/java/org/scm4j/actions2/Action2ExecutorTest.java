package org.scm4j.actions2;

import java.util.Map;
import java.util.UUID;

import org.scm4j.progress.IProgress;
import org.scm4j.progress.ProgressConsole;

import junit.framework.TestCase;

public class Action2ExecutorTest extends TestCase {
	
	static class A extends Action2Abstract{
		private Object result;
		public A(String name, Object result) {
			super(name);
			this.result = result;
		}
		@Override
		public Object execute(IAction2Executor executor, IProgress progress) throws Exception {
			return result;
		}
	}
	
	public void testEmpty() throws Exception{
		String name = UUID.randomUUID().toString();
		Action2Executor e = new Action2Executor(name, null);
		assertEquals(name, e.getName());
		assertEquals(0, e.getActionResults().size());
		assertEquals(0, e.getActions().size());
		assertEquals(0, e.queryActionResults(null, true).size());
		assertNull(e.execute(null, rp()));
	}
	
	ProgressConsole rp(){
		return new ProgressConsole("root");
	}
	
	public void testResults(){
		
		Action2Executor e = new Action2Executor("ex", null);
		
		final String res1 = UUID.randomUUID().toString();
		Integer res2 = 123;
		
		IAction2 a1 = new A("a1", res1);
		IAction2 a2 = new A("a2", res2);
		
		e.addAction(a1);
		e.addAction(a2);
		
		assertNull(e.execute(null, rp()));
		assertEquals(2, e.getActions().size());
		assertEquals("a1", e.getActions().get(0).getName());
		assertEquals("a2", e.getActions().get(1).getName());
		
		Map<IAction2, Object> res = e.getActionResults();
		assertEquals(2, res.size());
		assertEquals(res.get(a1), res1);
		assertEquals(res.get(a2), res2);
		
	}

}
