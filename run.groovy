@GrabResolver(name='jitpack', root='https://jitpack.io')
@Grab('com.github.scm4j:scm4j-releaser:2.2')

import org.scm4j.releaser.cli.CLI;

class CLIRunner {
	static void main(args) {
		System.exit(new CLI().exec(args));
	}
}
