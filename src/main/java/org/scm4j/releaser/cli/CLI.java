package org.scm4j.releaser.cli;

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
import org.scm4j.releaser.exceptions.cmdline.*;

import java.io.PrintStream;

public class CLI {
	public static final int EXIT_CODE_OK = 0;
	public static final int EXIT_CODE_ERROR = 1;

	private static PrintStream out = System.out;
	private static ActionTreeBuilder actionBuilder = new ActionTreeBuilder();
	private static ExtendedStatusBuilder statusBuilder = new ExtendedStatusBuilder();
	private IAction action;
	private RuntimeException lastException;

	private Runnable preExec = null;

	static void setOut(PrintStream out) {
		CLI.out = out;
	}
	
	static void setStatusTreeBuilder(ExtendedStatusBuilder statusBuilder) {
		CLI.statusBuilder = statusBuilder;
	}

	static void setActionBuilder(ActionTreeBuilder actionBuilder) {
		CLI.actionBuilder = actionBuilder;
	}

	public IAction getAction() {
		return action;
	}

	public void setPreExec(Runnable preExec) {
		this.preExec = preExec;
	}

	protected void printStatusTree(ExtendedStatus node) {
		PrintStatus ps = new PrintStatus();
		ps.print(out, node);
	}

	protected void execActionTree(IAction action) throws Exception {
		if (preExec != null) {
			preExec.run();
		}
		try (IProgress progress = new ProgressConsole(action.toStringAction(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
	}

	public ExtendedStatus getStatusTree(CommandLine cmd, CachedStatuses cache) {
		Component comp = new Component(cmd.getProductCoords());
		return comp.getVersion().isLocked() ?
				statusBuilder.getAndCachePatchStatus(comp, cache) :
				statusBuilder.getAndCacheMinorStatus(comp, cache);
	}
	
	private IAction getActionTree(ExtendedStatus node, CachedStatuses cache, CommandLine cmd) {
		if (cmd.getCommand() == CLICommand.BUILD) {
			return cmd.isDelayedTag() ?
					actionBuilder.getActionTreeDelayedTag(node, cache) :
					actionBuilder.getActionTreeFull(node, cache);
		}
		return actionBuilder.getActionTreeForkOnly(node, cache);
	}

	protected IAction getActionTree(CommandLine cmd) {
		CachedStatuses cache = new CachedStatuses();
		ExtendedStatus node = getStatusTree(cmd, cache);
		return getActionTree(node, cache, cmd);
	}

	protected IAction getTagAction(CommandLine cmd) {
		return actionBuilder.getTagAction(cmd.getProductCoords());
	}

	public int exec(String[] args) {
		try {
			out.println("scm4j-releaser " + CLI.class.getPackage().getSpecificationVersion());
			long startMS = System.currentTimeMillis();
			CommandLine cmd = new CommandLine(args);
			validateCommandLine(cmd);
			
			if (cmd.getCommand() == CLICommand.TAG) {
				action = getTagAction(cmd);
				execActionTree(action);
			} else {
				if (cmd.getCommand() == CLICommand.STATUS) {
					CachedStatuses cache = new CachedStatuses();
					ExtendedStatus node = getStatusTree(cmd, cache);
					printStatusTree(node);
				} else {
					action = getActionTree(cmd);
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

	void validateCommandLine(CommandLine cmd) {
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
		if (e instanceof RuntimeException) {
			lastException = (RuntimeException) e;
		}
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

	public RuntimeException getLastException() {
		return lastException;
	}
}