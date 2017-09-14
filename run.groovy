@GrabResolver(name='jitpack', root='https://jitpack.io')
@Grab('com.github.scm4j:scm4j-releaser:2.1')

import org.scm4j.releaser.cli.CLI;
import org.scm4j.releaser.exceptions.EConfig;

class CLIRunner {
	static void main(args) {
		try {
			CLI.main(args);
		} catch (EConfig e) {
		}
	}
}
