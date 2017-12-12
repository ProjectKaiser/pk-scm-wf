package org.scm4j.releaser;

import java.util.LinkedHashMap;

import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.releaser.conf.Component;

public class ExtendedStatusTreeNodeTest {
	
	@Test
	public void testToString() {
		new ExtendedStatusTreeNode(new Version(""), BuildStatus.BUILD, new LinkedHashMap<>(), new Component("comp")).toString();
	}
}
