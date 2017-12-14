package org.scm4j.releaser.cli;

import java.io.PrintStream;

import org.apache.commons.lang3.ArrayUtils;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.ActionTreeBuilder;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.actions.PrintAction;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.releaser.exceptions.cmdline.ECmdLine;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineNoCommand;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineNoProduct;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineUnknownCommand;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineUnknownOption;

public class CLI {

	public static final int EXIT_CODE_OK = 0;
	public static final int EXIT_CODE_ERROR = 1;

	private static ActionTreeBuilder actionBuilder = new ActionTreeBuilder();
	private static PrintStream out = System.out;

	static void setActionBuilder(ActionTreeBuilder actionBuilder) {
		CLI.actionBuilder = actionBuilder;
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

	public IAction getActionTree(CommandLine cmd) {
		switch (cmd.getCommand()) {
		case STATUS:
		case BUILD:
			return cmd.isDelayedTag() ? 
					actionBuilder.getActionTreeDelayedTag(cmd.getProductCoords()) :
					actionBuilder.getActionTree(cmd.getProductCoords());
		case FORK:
			return actionBuilder.getActionTreeForkOnly(cmd.getProductCoords());
		case TAG:
			return actionBuilder.getTagAction(cmd.getProductCoords());
		default:
			throw new IllegalArgumentException("Unsupported command: " + cmd.getCommand().toString());
		}
	}

	public int exec(String[] args) {
		try {
			CommandLine cmd = new CommandLine(args);
			validateCommandLine(cmd);
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
		for (String optionArg : cmd.getOptionArgs()) {
			if (Option.fromCmdLineStr(optionArg) == Option.UNKNOWN) {
				throw new ECmdLineUnknownOption(optionArg);
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
		if (ArrayUtils.contains(args, Option.STACK_TRACE.getCmdLineStr())) {
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