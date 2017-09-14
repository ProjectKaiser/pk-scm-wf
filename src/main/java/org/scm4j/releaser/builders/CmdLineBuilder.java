package org.scm4j.releaser.builders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.exceptions.EBuilder;

public class CmdLineBuilder implements IBuilder {

	private final String cmdLine;

	public CmdLineBuilder(String cmdLine) {
		this.cmdLine = cmdLine;
	}
	
	protected ProcessBuilder getProcessBuilder(String[] cmds) {
		return new ProcessBuilder(cmds);
	}

	@Override
	public void build(Component comp, File workingFolder, IProgress progress) throws Exception {
		progress.reportStatus(String.format("executing \"%s\" in folder %s", cmdLine, workingFolder.getPath()));

		String[] cmds = cmdLine.split(" ");
		
		ProcessBuilder pb = getProcessBuilder(cmds);
		
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.INHERIT);
		pb.redirectError(Redirect.INHERIT);
		pb.directory(workingFolder);
		Process process = pb.start();

		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}

		if (process.waitFor() != 0) {
			try (InputStream is = process.getErrorStream()) {
				throw new EBuilder(IOUtils.toString(is, StandardCharsets.UTF_8));
			}
		}
	}

	public String getCmdLine() {
		return cmdLine;
	}


}
