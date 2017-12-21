package org.scm4j.releaser.cli;

public class CLIFactory implements ICLIFactory {
	@Override
	public CLI getCLI() {
		return new CLI();
	}
}
