package org.scm4j.releaser.exceptions.cmdline;

public class ECmdLineUnknownCommand extends ECmdLine {

	private static final long serialVersionUID = 1L;

	public ECmdLineUnknownCommand(String cmd) {
		super("unknown command: " + cmd);
	}

}
