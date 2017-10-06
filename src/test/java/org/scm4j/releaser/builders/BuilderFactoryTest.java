package org.scm4j.releaser.builders;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.scm4j.releaser.TestBuilder;

public class BuilderFactoryTest {
	
	@Test
	public void testGetBuilder() {
		assertTrue(BuilderFactory.getBuilder("cmd line builder") instanceof CmdLineBuilder);
		
		try {
			BuilderFactory.getBuilder(BuilderFactory.SCM4J_BUILDER_CLASS_STRING + "wrong builder class");
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.getCause() instanceof ClassNotFoundException);
		}
		
		assertTrue(BuilderFactory.getBuilder(BuilderFactory.SCM4J_BUILDER_CLASS_STRING + TestBuilder.class.getName()) instanceof TestBuilder);
	}

}
