package org.scm4j.releaser.exceptions.cmdline;

public class ECmdLineNoCommand extends ECmdLine {

	private static final long serialVersionUID = 1L;

	public ECmdLineNoCommand() {
		super("command is not specified");
	}

}
