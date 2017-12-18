package org.scm4j.releaser.cli;

import java.io.PrintStream;

import org.apache.commons.lang3.ArrayUtils;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.ActionTreeBuilder;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.ExtendedStatusBuilder;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.actions.PrintStatus;
import org.scm4j.releaser.conf.Component;
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

	private void printStatusTree(ExtendedStatus node) {
		PrintStatus ps = new PrintStatus();
		ps.print(out, node);
	}

	public void execActionTree(IAction action) throws Exception {
		try (IProgress progress = new ProgressConsole(action.toStringAction(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
	}
	
	public ExtendedStatus getStatusTree(CommandLine cmd, CachedStatuses cache) {
		ExtendedStatusBuilder statusBuilder = new ExtendedStatusBuilder();
		Component comp = new Component(cmd.getProductCoords());
		return !comp.getVersion().isLocked() && cmd.getCommand() == CLICommand.BUILD ?
				statusBuilder.getAndCacheMinorStatus(comp, cache) :
				statusBuilder.getAndCachePatchStatus(comp, cache);
	}
	
	public IAction getActionTree(ExtendedStatus node, CachedStatuses cache, CommandLine cmd) {
		if (cmd.getCommand() == CLICommand.BUILD) {
			return cmd.isDelayedTag() ? 
					actionBuilder.getActionTreeDelayedTag(node, cache) :
					actionBuilder.getActionTreeFull(node, cache);
		} else {
			return actionBuilder.getActionTreeForkOnly(node, cache);
		}
	}
	
	public IAction getActionTree(CommandLine cmd) {
		if (cmd.getCommand() == CLICommand.TAG) {
			return actionBuilder.getTagAction(cmd.getProductCoords());
		} else {
			CachedStatuses cache = new CachedStatuses();
			ExtendedStatus node = getStatusTree(cmd, cache);
			return getActionTree(node, cache, cmd);
		}
	}

	public int exec(String[] args) {
		try {
			long startMS = System.currentTimeMillis();
			CommandLine cmd = new CommandLine(args);
			validateCommandLine(cmd);
			IAction action = getActionTree(cmd);
			execActionTree(action, cmd);
			if (cmd.getCommand() == CLICommand.TAG) {
				return actionBuilder.getTagAction(cmd.getProductCoords());
			} else {
				CachedStatuses cache = new CachedStatuses();
				ExtendedStatus node = getStatusTree(cmd, cache);
				if (cmd.getCommand() == CLICommand.STATUS) {
					printStatusTree(node);
				} else {
					IAction action = getActionTree(node, cache, cmd);
					execActionTree(action);
				}
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