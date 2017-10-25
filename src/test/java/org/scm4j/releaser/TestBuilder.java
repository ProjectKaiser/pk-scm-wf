package org.scm4j.releaser;

import java.io.File;
import java.util.Map;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.builders.IBuilder;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.exceptions.EBuilder;

public class TestBuilder implements IBuilder {

	private static Map<String, TestBuilder> builders;

	public static Map<String, TestBuilder> getBuilders() {
		return builders;
	}

	public static void setBuilders(Map<String, TestBuilder> builders) {
		TestBuilder.builders = builders;
	}

	@Override
	public void build(Component comp, File workingFolder, IProgress progress ) throws EBuilder {
		builders.put(comp.getName(), this);
	}

	@Override
	public String getCommand() {
		return null;
	}
}
