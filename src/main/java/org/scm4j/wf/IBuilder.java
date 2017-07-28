package org.scm4j.wf;

import java.io.File;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.exceptions.EBuilder;

public interface IBuilder {
	void build(Component comp, File workingFolder, IProgress progress) throws Exception;
}
