@GrabResolver(name='jitpack', root='https://jitpack.io')
@Grab('com.github.scm4j:scm4j-wf:dev-SNAPSHOT')

import org.scm4j.wf.SCMWorkflow;
import org.scm4j.wf.actions.IAction;
import org.scm4j.wf.actions.PrintAction;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.wf.actions.ActionKind;
import org.scm4j.wf.conf.Component;
//import org.apache.commons.cli.Option;

class CLI {

	static void main(args) {

		def cli = new CliBuilder(usage: 'groovy run.groovy -status|-fork|-build|-tag productCoords [--delayed-tag]')
		
		if (args.length < 2) {
			printUsage()
			System.exit(1)
		}
		
		def cmd = args[0]
		
		def productCoords = args[1]
		
		def options;
		if (args.length > 2) {
			options = args.subList(2, args.length)
		}
		
		println options
		
/*		
		args.each {
			switch (it) {
				case "status":
					SCMWorkflow wf = new SCMWorkflow()
					IAction action = wf.getProductionReleaseAction(opt.show);
					PrintAction pa = new PrintAction();
					pa.print(System.out, action);
					break;
				case "fork":
				

		cli.status('show actions will be made with product specified by productCoords', required: false, args: 1, argName: 'productCoords', type: String)
		cli.fork('cerate all necessary release branches for product specified by productCoords', required: false, args: 1, argName: 'productCoords', type:String)
		cli.build('execute production release action on product specified by productCoords', required: false, args: 1, argName: 'productCoords', type: String)
		cli.tag('execute tag action on product specified by productCoords', required: false, args: 1, argName: 'productCoords', type: String)

		OptionAccessor opt = cli.parse(args)
		if(!opt) {
			return
		}
		
		if (opt.status) {
			
		} else if (opt.fork) {
			SCMWorkflow wf = new SCMWorkflow()
			IAction action = wf.getProductionReleaseAction(opt.fork, ActionKind.FORK);
			IProgress progress = new ProgressConsole(System.out, ">>> ", "<<< ");
			Object res = action.execute(progress);
			progress.close();
			if (res instanceof Throwable) {
				System.exit(1);
			}
		} else if (opt.build) {
			SCMWorkflow wf = new SCMWorkflow()
			IAction action = wf.getProductionReleaseAction(opt.fork, ActionKind.BUILD);
			IProgress progress = new ProgressConsole(System.out, ">>> ", "<<< ");
			Object res = action.execute(progress);
			progress.close();
			if (res instanceof Throwable) {
				System.exit(1);
			}
		} else if (opt.tag) {
			SCMWorkflow wf = new SCMWorkflow()
			IAction action = wf.getTagReleaseAction(new Component(opt.fork));
			IProgress progress = new ProgressConsole(System.out, ">>> ", "<<< ");
			Object res = action.execute(progress);
			progress.close();
			if (res instanceof Throwable) {
				System.exit(1);
			}
		} else {
			cli.usage()
		}
		*/
	}
	
	private static void printUsage() {
		println 'usage:  groovy run.groovy status|fork|build|tag productCoords [--delayed-tag]'
		println ''
		println 'status: show actions will be made with product specified by productCoords'
		println 'fork:   cerate all necessary release branches for product specified by productCoords'
		println 'build:  execute production release action on product specified by productCoords. if --delayed-tag option is provided then tag will not be applied to a new relase commit. Use tag command to apply delayed tags'
		println 'tag:    apply delayed tags on product specified by productCoords'
	}
}
