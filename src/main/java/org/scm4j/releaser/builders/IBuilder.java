package org.scm4j.releaser.builders;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.conf.Component;

import java.io.File;
import java.util.Map;

public interface IBuilder {
	void build(Component comp, File workingFolder, IProgress progress, Map<String, String> buildTimeEnvVars) throws Exception;
	String getCommand();
}
