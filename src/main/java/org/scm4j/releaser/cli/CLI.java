package org.scm4j.releaser.cli;

import org.apache.commons.lang3.ArrayUtils;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.actions.ActionSet;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.actions.PrintAction;
import org.scm4j.releaser.conf.Option;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.releaser.exceptions.cmdline.*;

import java.io.PrintStream;

public class CLI {
	
	public static final int EXIT_CODE_OK = 0;
	public static final int EXIT_CODE_ERROR = 1;
	
	private static SCMReleaser releaser = new SCMReleaser();
	private static PrintStream out = System.out;

	static void setReleaser(SCMReleaser releaser) {
		CLI.releaser = releaser;
				}

	static void setOut(PrintStream out) {
		CLI.out = out;
			}

	private void printActionTree(IAction action) {
		PrintAction pa = new PrintAction();
		pa.print(out, action);
		} 
		
	public void execActionTree(IAction action) throws Exception {
		try (IProgress progress = new ProgressConsole(action.toStringAction(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		}

	public IAction getActionTree(CommandLine cmd) throws Exception {
		switch(cmd.getCommand()) {
			case STATUS:
		case BUILD:
				return releaser.getActionTree(cmd.getProductCoords(), ActionSet.FULL);
		case FORK:
				return releaser.getActionTree(cmd.getProductCoords(), ActionSet.FORK_ONLY);
		case TAG:
				return releaser.getTagActionTree(cmd.getProductCoords());
			default:
				throw new IllegalArgumentException("Unsupported command: " + cmd.getCommand().toString());
			}
		}
	
	public int exec(String[] args) {
		try {
			CommandLine cmd = new CommandLine(args);
			validateCommandLine(cmd);
			Options.parse(cmd.getOptionArgs());
			long startMS = System.currentTimeMillis();
			IAction action = getActionTree(cmd);
			if (cmd.getCommand() == CLICommand.STATUS) {
				printActionTree(action);
			} else {
				execActionTree(action);
			}
			out.println("elapsed time: " + (System.currentTimeMillis() - startMS));
			return EXIT_CODE_OK;
		} catch (ECmdLine e) {
			printException(args, e, out);
			out.println(CommandLine.getUsage());
			return EXIT_CODE_ERROR;
		} catch (Exception e) {
			printException(args, e, out);
			return EXIT_CODE_ERROR;
		}
	}
	
	public void validateCommandLine(CommandLine cmd) {
		if (cmd.getArgs().length > 2) {
			String[] optionArgs = cmd.getOptionArgs();
			for (String optionArg : optionArgs) {
				if (!Option.isValid(optionArg)) {
					throw new ECmdLineUnknownOption(optionArg);
	}
			}
		}

		if (cmd.getCommand() == null) {
			throw new ECmdLineNoCommand();
		}

		if (cmd.getCommand() == CLICommand.UNKNOWN) {
			throw new ECmdLineUnknownCommand(cmd.getCommandStr());
		}

		if (cmd.getProductCoords() == null) {
			throw new ECmdLineNoProduct();
		}
	}

	private void printException(String[] args, Exception e, PrintStream ps) {
		if (ArrayUtils.contains(args, Option.STACK_TRACE.getStrValue())) {
			e.printStackTrace(ps);
		} else {
			if (e instanceof EReleaserException) {
				ps.println(e.getMessage() + (e.getCause() != null ? ": " + e.getCause().toString() : "")); 
			} else {
				ps.println(e.toString());
			} 
		}
	}
	
	public static void main(String[] args) throws Exception {
		System.exit(new CLI().exec(args));
	}
}