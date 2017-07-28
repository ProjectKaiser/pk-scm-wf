package org.scm4j.wf;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.wf.conf.Component;
import org.scm4j.wf.exceptions.EBuilder;

public class CmdLineBuilder implements IBuilder {

	private final String cmdLine;

	@Override
	public void build(Component comp, File workingFolder, IProgress progress) throws Exception {
		progress.reportStatus(String.format("executing \"%s\" in folder %s", cmdLine, workingFolder.getPath()));
		//args[0] = new File(workingFolder, args[0]).getPath();
//		String cmd = args[0];
//		args = Arrays.copyOfRange(args, 1, args.length);
		String[] cmds = cmdLine.split(" ");
		String[] args = new String[cmds.length + 1];
		args[0] = "//c";
		Integer i = 1;
		for (String s : cmds) {
			args[i] = s;
			i++;
		}
		Process proc = Runtime.getRuntime().exec("cmd", args, workingFolder);
		try (InputStream is = proc.getInputStream();
			 InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)) {
			String line; 
			while ((line = br.readLine()) != null) {
	            progress.reportStatus(line);
	        }
			//progress.reportStatus(IOUtils.toString(is, StandardCharsets.UTF_8));
		}
		if (proc.waitFor() != 0) {
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
