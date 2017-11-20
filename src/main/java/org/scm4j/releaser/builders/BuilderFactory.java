package org.scm4j.releaser.builders;

public final class BuilderFactory {
	
	public static final String SCM4J_BUILDER_CLASS_STRING = "scm4j-builder-class:";

	public static IBuilder getBuilder(String releaseCommand) {
		try { 
			if (releaseCommand == null) {
				return null;
			}
			if (releaseCommand.startsWith(SCM4J_BUILDER_CLASS_STRING)) {
				Class<?> builderClass = Class.forName(releaseCommand.replace(SCM4J_BUILDER_CLASS_STRING, "").trim());
				return (IBuilder) builderClass.newInstance(); 
			}
			return new CmdLineBuilder(releaseCommand);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private BuilderFactory() {
	}
}
