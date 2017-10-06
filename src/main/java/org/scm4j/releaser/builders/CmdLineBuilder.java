package org.scm4j.releaser.builders;

import org.apache.commons.io.IOUtils;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.exceptions.EBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CmdLineBuilder implements IBuilder {

	private final String cmdLine;
	private ProcessBuilder pb;
	private static final Pattern CMD_LINE_PATTERN = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

	public CmdLineBuilder(String cmdLine) {
		this.cmdLine = cmdLine;
		Matcher m = CMD_LINE_PATTERN.matcher(cmdLine);
		List<String> cmds = new ArrayList<>();
		while (m.find()) {
			cmds.add(m.group(1));
		}
		pb = new ProcessBuilder(cmds);
	}

	protected Process getProcess(File workingFolder) throws Exception {
		pb.redirectErrorStream(true);
		pb.directory(workingFolder);
		return pb.start();
	}

	@Override
	public void build(Component comp, File workingFolder, IProgress progress) throws Exception {
		progress.reportStatus(String.format("executing \"%s\" in folder %s", cmdLine, workingFolder.getPath()));
		Process process = getProcess(workingFolder);
		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line;
		while ((line = br.readLine()) != null) {
			progress.reportStatus(line);
		}

		if (process.waitFor() != 0) {
			InputStream is = process.getErrorStream();
			throw new EBuilder(IOUtils.toString(is, StandardCharsets.UTF_8), comp);
		}
	}
	
	public ProcessBuilder getProcessBuilder() {
		return pb;
	}
}
