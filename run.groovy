@GrabResolver(name='jitpack', root='https://jitpack.io')
@Grab('com.github.scm4j:scm4j-wf:dev-SNAPSHOT')

import org.scm4j.wf.cli.CLI;
import org.scm4j.wf.exceptions.EConfig;

class CLIRunner {
	static void main(args) {
		try {
			CLI.main(args);
		} catch (EConfig e) {
		}
	}
}
