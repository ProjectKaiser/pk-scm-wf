package org.scm4j.releaser.cli;

import org.apache.commons.lang3.ArrayUtils;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.actions.ActionKind;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.actions.PrintAction;
import org.scm4j.releaser.conf.Option;
import org.scm4j.releaser.conf.Options;
import org.scm4j.releaser.exceptions.EReleaserException;
import org.scm4j.releaser.exceptions.cmdline.*;

import java.io.PrintStream;
import java.util.Arrays;

public class CLI {
	
	public static final int EXIT_CODE_OK = 0;
	public static final int EXIT_CODE_ERROR = 1;
	
	public void execInternal(SCMReleaser releaser, CommandLine cmd, PrintStream ps) throws Exception {
		if (cmd.getArgs().length > 2) {
			String[] optionArgs = Arrays.copyOfRange(cmd.getArgs(), 2, cmd.getArgs().length);
			for (String optionArg : optionArgs) {
				if (!Option.isValid(optionArg)) {
					throw new ECmdLineUnknownOption(optionArg);
				}
			}
			Options.parse(optionArgs);
		} 
		
		if (cmd.getCommand() == null) {
			throw new ECmdLineNoCommand();
		}
		if (cmd.getProductCoords() == null) {
			throw new ECmdLineNoProduct();
		}
		IAction action;
		long startMS = System.currentTimeMillis();
		switch(cmd.getCommand()) {
		case BUILD:
			action = releaser.getActionTree(cmd.getProductCoords(), ActionKind.FULL);
			try (IProgress progress = new ProgressConsole(action.toStringAction(), ">>> ", "<<< ")) {
				action.execute(progress);
			}
			break;
		case FORK:
			action = releaser.getActionTree(cmd.getProductCoords(), ActionKind.FORK_ONLY);
			try (IProgress progress = new ProgressConsole(action.toStringAction(), ">>> ", "<<< ")) {
				action.execute(progress);
			}
			break;
		case STATUS:
			action = releaser.getActionTree(cmd.getProductCoords());
			PrintAction pa = new PrintAction();
			pa.print(ps, action);
			break;
		case TAG:
			action = releaser.getActionTree(cmd.getProductCoords());
			try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
				action.execute(progress);
			}
			break;
		case UNKNOWN: 
			throw new ECmdLineUnknownCommand(cmd.getCommandStr());
		}
		System.out.println("elapsed time: " + (System.currentTimeMillis() - startMS));
	}
	
	public int exec(SCMReleaser releaser, CommandLine cmd, PrintStream ps) {
		try {
			execInternal(releaser, cmd, ps);
			return EXIT_CODE_OK;
		} catch (ECmdLine e) {
			printException(cmd.getArgs(), e, ps);
			ps.println(CommandLine.getUsage());
			return EXIT_CODE_ERROR;
		} catch (Exception e) {
			printException(cmd.getArgs(), e, ps);
			return EXIT_CODE_ERROR;
		}
	}
	
	public int exec(String[] args) throws Exception {
		return exec(new SCMReleaser(), new CommandLine(args), System.out);
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