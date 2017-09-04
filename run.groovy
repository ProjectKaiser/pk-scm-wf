@GrabResolver(name='jitpack', root='https://jitpack.io')
@Grab('com.github.scm4j:scm4j-wf:master-SNAPSHOT')

import  org.scm4j.wf.cli.CLI;

class CLIRunner {
	static void main(args) {
		CLI.main(args);
		}
			}
