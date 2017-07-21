package org.scm4j.wf.conf;

import org.scm4j.wf.CmdLineBuilder;
import org.scm4j.wf.IBuilder;

public class BuilderFactory {
	
	private static final String SCM4J_BUILDER_CLASS = "scm4j-builder-class:";

	public static IBuilder getBuilder(String builder) {
		try { 
			if (builder == null) {
				return null;
			}
			if (builder.startsWith(SCM4J_BUILDER_CLASS)) {
				Class<?> builderClass = Class.forName(builder.replace(SCM4J_BUILDER_CLASS, "").trim());
				return (IBuilder) builderClass.newInstance(); 
			}
			return new CmdLineBuilder(builder);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

}
