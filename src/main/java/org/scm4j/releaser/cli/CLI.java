package org.scm4j.releaser.cli;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.fusesource.jansi.AnsiConsole;
import org.scm4j.commons.coords.Coords;
import org.scm4j.commons.coords.CoordsGradle;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.*;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.actions.PrintStatus;
import org.scm4j.releaser.conf.DefaultConfigUrls;
import org.scm4j.releaser.conf.IConfigUrls;
import org.scm4j.releaser.conf.VCSRepositoryFactory;
import org.scm4j.releaser.exceptions.cmdline.*;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

public class CLI {
	public static final String CONFIG_TEMPLATES_ROSURCE_PATH = "config-templates/";
	public static final int EXIT_CODE_OK = 0;
	public static final int EXIT_CODE_ERROR = 1;
	public static final List<String> CONFIG_TEMPLATES = Arrays.asList("cc", "cc.yml", "credentials.yml");
	public static final String EXECUTION_FAILED_MESSAGE = "EXECUTION FAILED: ";

	private final PrintStream out;
	private final ActionTreeBuilder actionBuilder;
	private final ExtendedStatusBuilder statusBuilder;
	private final IConfigUrls configUrls;
	private final VCSRepositoryFactory repoFactory;
	private IAction action;
	private RuntimeException lastException = null;
	private Runnable preExec = null;

	public CLI() {
		this(System.out, new DefaultConfigUrls());
	}

	public CLI(PrintStream out, IConfigUrls configUrls) {
		this.out = out;
		repoFactory = new VCSRepositoryFactory();
		this.statusBuilder = new ExtendedStatusBuilder(repoFactory);
		this.actionBuilder = new ActionTreeBuilder(repoFactory);
		this.configUrls = configUrls;

	}

	public CLI(PrintStream out, ExtendedStatusBuilder statusBuilder, ActionTreeBuilder actionBuilder, VCSRepositoryFactory repoFactory) {
		this.out = out;
		AnsiConsole.wrapOutputStream(out);
		this.statusBuilder = statusBuilder;
		this.actionBuilder = actionBuilder;
		this.repoFactory = repoFactory;
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
		if (action.isExecutable()) {
			try (IProgress progress = new ProgressConsole(action.toStringAction(), ">>> ", "<<< ")) {
				action.execute(progress);
			}
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
				CommandLine cmd = new CommandLine(args);
				validateCommandLine(cmd);

				long startMS = System.currentTimeMillis();
				try {
					repoFactory.load(configUrls);
				} catch (Exception e) {
					printExceptionConfig(args, e, out);
					return EXIT_CODE_ERROR;
				}
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
		if (configUrls.getCCUrls() != null || configUrls.getCredsUrl() != null) {
			return;
		}
		Utils.BASE_WORKING_DIR.mkdirs();
		for (String ct : CONFIG_TEMPLATES) {
			File ctFile = new File(Utils.BASE_WORKING_DIR, ct);
			if (!ctFile.exists()) {
				InputStream is = this.getClass().getResourceAsStream(CONFIG_TEMPLATES_ROSURCE_PATH + ct);
				FileUtils.copyInputStreamToFile(is, ctFile);
			}
		}
	}

	void validateCommandLine(CommandLine cmd) {
		for (String optionArg : cmd.getOptionArgs()) {
			if (Option.fromCmdLineStr(optionArg) == null) {
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
		printException(EXECUTION_FAILED_MESSAGE, args, e, ps);
	}

	void printExceptionInitDir(String[] args, Exception e, PrintStream ps) {
		printException("FAILED TO INIT WORKING FOLDER: ", args, e, ps);
	}

	void printExceptionConfig(String[] args, Exception e, PrintStream ps) {
		printException("FAILED TO LOAD CONFIG: ", args, e, ps);
	}

	private void printException(String prefixMessage, String[] args, Exception e, PrintStream ps) {
		if (ArrayUtils.contains(args, Option.STACK_TRACE.getCmdLineStr())) {
			ps.println(ansi().fgBrightRed().a(prefixMessage).reset().toString());
			e.printStackTrace(ps);
		} else {
			ps.println(ansi().fgBrightRed().a(prefixMessage + (e.getMessage() == null ? e.toString() : e.getMessage()))
					.reset().toString());
		}
	}

	public static void main(String[] args) throws Exception {
		AnsiConsole.systemInstall();
		System.exit(new CLI().exec(args));
	}

	public RuntimeException getLastException() {
		return lastException;
	}
}