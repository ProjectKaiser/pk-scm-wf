package org.scm4j.wf.builders;

import java.io.File;

import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.conf.Component;

public interface IBuilder {
	void build(Component comp, File workingFolder, IProgress progress) throws Exception;
}
