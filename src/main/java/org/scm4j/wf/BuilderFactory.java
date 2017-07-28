package org.scm4j.wf;

public class BuilderFactory {
	
	public static final String SCM4J_BUILDER_CLASS_STRING = "scm4j-builder-class:";

	public static IBuilder getBuilder(String builder) {
		try { 
			if (builder == null) {
				return null;
			}
			if (builder.startsWith(SCM4J_BUILDER_CLASS_STRING)) {
				Class<?> builderClass = Class.forName(builder.replace(SCM4J_BUILDER_CLASS_STRING, "").trim());
				return (IBuilder) builderClass.newInstance(); 
			}
			return new CmdLineBuilder(builder);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

}
