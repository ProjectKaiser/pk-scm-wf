@GrabResolver(name = 'jitpack', root = 'https://jitpack.io', changing = true, m2Compatible = true)
@Grab(group = 'com.github.scm4j', module = 'scm4j-wf', version = 'master-SNAPSHOT', changing = true)

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

		def cli = new CliBuilder(usage: 'groovy run.groovy -show|-fork|-build|-tag productCoords')

		cli.show('show actions which will be made with product specified by productCoords', required: false, args: 1, argName: 'productCoords', type: String)
		cli.fork('create all necessary release branches for product specified by productCoords', required: false, args: 1, argName: 'productCoords', type:String)
		cli.build('execute production release action on product specified by productCoords', required: false, args: 1, argName: 'productCoords', type: String)
		cli.tag('execute tag action on product specified by productCoords', required: false, args: 1, argName: 'productCoords', type: String)

		OptionAccessor opt = cli.parse(args)
		if(!opt) {
			return
		}
		
		if (opt.show) {
			SCMWorkflow wf = new SCMWorkflow()
			IAction action = wf.getProductionReleaseAction(opt.show);
			PrintAction pa = new PrintAction();
			pa.print(System.out, action);
		} else if (opt.fork) {
			SCMWorkflow wf = new SCMWorkflow()
			IAction action = wf.getProductionReleaseAction(opt.fork, ActionKind.FORK);
			IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ");
			Object res = action.execute(progress);
			progress.close();
			if (res instanceof Throwable) {
				System.exit(1);
			}
		} else if (opt.build) {
			SCMWorkflow wf = new SCMWorkflow()
			IAction action = wf.getProductionReleaseAction(opt.fork, ActionKind.BUILD);
			IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ");
			Object res = action.execute(progress);
			progress.close();
			if (res instanceof Throwable) {
				System.exit(1);
			}
		} else if (opt.tag) {
			SCMWorkflow wf = new SCMWorkflow()
			IAction action = wf.getTagReleaseAction(new Component(opt.fork));
			IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ");
			Object res = action.execute(progress);
			progress.close();
			if (res instanceof Throwable) {
				System.exit(1);
			}
		} else {
			cli.usage()
		}
	}
}
