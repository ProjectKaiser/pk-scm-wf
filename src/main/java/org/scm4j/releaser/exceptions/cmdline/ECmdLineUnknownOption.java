package org.scm4j.releaser.exceptions.cmdline;

public class ECmdLineUnknownOption extends ECmdLine {

	private static final long serialVersionUID = 1L;

	public ECmdLineUnknownOption(String option) {
		super("unknown option: " + option);
	}

}
