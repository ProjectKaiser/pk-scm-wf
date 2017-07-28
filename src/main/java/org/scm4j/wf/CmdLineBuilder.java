package org.scm4j.wf;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.exceptions.EBuilder;

public class CmdLineBuilder implements IBuilder {

	private final String cmdLine;

	@Override
	public void build(Component comp, File workingFolder, IProgress progress) throws Exception {
		Process proc;
		proc = Runtime.getRuntime().exec(cmdLine, new String[0], workingFolder);
		proc.waitFor();
		try (InputStream is = Runtime.getRuntime().exec(cmdLine, new String[0], workingFolder).getInputStream()) {
			progress.reportStatus(IOUtils.toString(is, StandardCharsets.UTF_8));
		}
		if (proc.exitValue() != 0) {
			try (InputStream is = proc.getErrorStream()) {
				throw new EBuilder(IOUtils.toString(is, StandardCharsets.UTF_8));
			}
		}
	}

	public CmdLineBuilder(String cmdLine) {
		this.cmdLine = cmdLine;
	}

	public String getCmdLine() {
		return cmdLine;
	}
}
