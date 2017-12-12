package org.scm4j.releaser;

import java.util.LinkedHashMap;

import org.junit.Test;
import org.scm4j.commons.Version;

public class ExtendedStatusTreeNodeTest extends WorkflowTestBase {
	
	@Test
	public void testToString() {
		new ExtendedStatusTreeNode(new Version(""), BuildStatus.BUILD, new LinkedHashMap<>(), compUnTill).toString();
	}
}
