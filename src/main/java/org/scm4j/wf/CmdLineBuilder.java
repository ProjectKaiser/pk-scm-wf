package org.scm4j.wf;

import java.io.File;

import org.scm4j.wf.exceptions.EBuilder;

public class CmdLineBuilder implements IBuilder {
	
	private final String cmdLine;

	@Override
	public void build(File workingFolder) throws EBuilder {
		try {
			Runtime.getRuntime().exec(cmdLine, new String[0], workingFolder);
		} catch (Exception e) {
			throw new EBuilder(e);
		}
	}
	
	public CmdLineBuilder(String cmdLine) {
		this.cmdLine = cmdLine;
	}

	public String getCmdLine() {
		return cmdLine;
	}
}
