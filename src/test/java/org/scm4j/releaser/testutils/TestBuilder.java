package org.scm4j.releaser.testutils;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.builders.IBuilder;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.exceptions.EBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TestBuilder implements IBuilder {

	private static Map<String, TestBuilder> builders;

	public static Map<String, Map<String, String>> getEnvVars() {
		return envVars;
	}

	private static Map<String, Map<String, String>> envVars = new HashMap<>();

	public static Map<String, TestBuilder> getBuilders() {
		return builders;
	}

	public static void setBuilders(Map<String, TestBuilder> builders) {
		TestBuilder.builders = builders;
	}

	@Override
	public void build(Component comp, File workingFolder, IProgress progress, Map<String, String> buildTimeEnvVars) throws EBuilder {
		builders.put(comp.getName(), this);
		envVars.put(comp.getName(), buildTimeEnvVars);
	}

	@Override
	public String getCommand() {
		return null;
	}
}
