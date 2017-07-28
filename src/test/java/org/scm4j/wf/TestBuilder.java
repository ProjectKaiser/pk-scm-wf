package org.scm4j.wf;

import java.io.File;
import java.util.Map;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.exceptions.EBuilder;

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
}
