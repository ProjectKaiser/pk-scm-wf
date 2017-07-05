package org.scm4j.wf;

@GrabResolver(name = 'jitpack', root = 'https://jitpack.io', changing = true, m2Compatible = true)
@Grab(group = 'com.github.scm4j', module = 'scm4j-wf', version = 'master-SNAPSHOT', changing = true)

import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.PrintAction;

class CLI {

	static void main(args) {

		def cli = new CliBuilder(usage: 'groovy run.groovy -show|-build|-tag productCoords')

		cli.show('show actions will be made with product specified by productCoords', required: false, args: 1, argName: 'productCoords', type: String)
		cli.build('execute production release action on product specified by productCoords', required: false, args: 1, argName: 'productCoords', type: String)
		cli.tag('execute tag action on product specified by productCoords', required: false, args: 1, argName: 'productCoords', type: String)

		OptionAccessor opt = cli.parse(args)
		if(!opt) {
			return
		}
		
		if (opt.show) {
			SCMWorkflow wf = new SCMWorkflow(opt.show)
			IAction action = wf.getProductionReleaseAction(null);
			PrintAction pa = new PrintAction();
			pa.print(System.out, action);
		} else if (opt.build) {

		} else if (opt.tag) {

		} else {
			cli.usage()
		}
	}
}
