package org.scm4j.releaser.cli;

import java.io.File;
import java.io.PrintStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.scm4j.commons.coords.Coords;
import org.scm4j.commons.coords.CoordsGradle;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.ActionTreeBuilder;
import org.scm4j.releaser.CachedStatuses;
import org.scm4j.releaser.ExtendedStatus;
import org.scm4j.releaser.ExtendedStatusBuilder;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.actions.PrintStatus;
import org.scm4j.releaser.conf.DefaultConfigUrls;
import org.scm4j.releaser.conf.IConfigUrls;
import org.scm4j.releaser.conf.VCSRepositoryFactory;
import org.scm4j.releaser.exceptions.cmdline.ECmdLine;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineNoCommand;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineNoProduct;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineUnknownCommand;
import org.scm4j.releaser.exceptions.cmdline.ECmdLineUnknownOption;

public class CLI {
	public static final String CONFIG_TEMPLATES = "config-templates";
	public static final int EXIT_CODE_OK = 0;
	public static final int EXIT_CODE_ERROR = 1;

	private final PrintStream out;
	private final ActionTreeBuilder actionBuilder;
	private final ExtendedStatusBuilder statusBuilder;
	private IAction action;
	private RuntimeException lastException;
	private Runnable preExec = null;
	private IConfigUrls configUrls;
	
	public CLI() {
		this(System.out, new DefaultConfigUrls());
	}
	
	public CLI(PrintStream out, IConfigUrls configUrls) {
		this.out = out;
		VCSRepositoryFactory repoFactory = new VCSRepositoryFactory();
		repoFactory.load(configUrls);
		this.statusBuilder = new ExtendedStatusBuilder(repoFactory);
		this.actionBuilder = new ActionTreeBuilder(repoFactory);
		this.configUrls = configUrls;
	}
	
	public CLI(PrintStream out, ExtendedStatusBuilder statusBuilder, ActionTreeBuilder actionBuilder) {
		this.out = out;
		this.statusBuilder = statusBuilder;
		this.actionBuilder = actionBuilder;
		this.configUrls = new DefaultConfigUrls();
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
		Coords coords = new CoordsGradle(cmd.getProductCoords());
		return coords.getVersion().isLocked() ?
				statusBuilder.getAndCachePatchStatus(cmd.getProductCoords(), cache) :
				statusBuilder.getAndCacheMinorStatus(cmd.getProductCoords(), cache);
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
			try {
				out.println("scm4j-releaser " + CLI.class.getPackage().getSpecificationVersion());
				try {
					initWorkingDir();
				} catch (Exception e) {
					printExceptionInitDir(args, e, out);
				}
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
			} catch (RuntimeException e) {
				lastException = e;
				throw e;
			}
		} catch (ECmdLine e) {
			printExceptionCmdLine(args, e, out);
			out.println(CommandLine.getUsage());
			return EXIT_CODE_ERROR;
		} catch (Exception e) {
			printExceptionExecution(args, e, out);
			return EXIT_CODE_ERROR;
		}
	}

	void initWorkingDir() throws Exception {
		if (Utils.BASE_WORKING_DIR.exists() || configUrls.getCCUrls() != null || configUrls.getCredsUrl() != null) {
			return;
		}
		
		Utils.BASE_WORKING_DIR.mkdirs();
		File resourcesFrom = Utils.getResourceFile(this.getClass(), CONFIG_TEMPLATES);
		FileUtils.copyDirectory(resourcesFrom, Utils.BASE_WORKING_DIR);
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
	
	void printExceptionCmdLine(String[] args, Exception e, PrintStream ps) {
		printException("", args, e, ps);
	}
	
	void printExceptionExecution(String[] args, Exception e, PrintStream ps) {
		printException("EXECUTION FIALED: ", args, e, ps);
	}
	
	void printExceptionInitDir(String[] args, Exception e, PrintStream ps) {
		printException("FAILED TO INIT WORKING FOLDER: ", args, e, ps);
	}
	
	private void printException(String prefixMessage, String[] args, Exception e, PrintStream ps) {
		ps.println();
		if (ArrayUtils.contains(args, Option.STACK_TRACE.getCmdLineStr())) {
			ps.println(prefixMessage);
			e.printStackTrace(ps);
		} else {
			ps.println(prefixMessage + (e.getMessage() == null ? e.toString() : e.getMessage()));
		}
	}

	public static void main(String[] args) throws Exception {
		System.exit(new CLI().exec(args));
	}

	public RuntimeException getLastException() {
		return lastException;
	}
}