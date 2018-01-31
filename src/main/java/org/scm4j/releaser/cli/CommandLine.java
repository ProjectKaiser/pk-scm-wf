package org.scm4j.releaser.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandLine {

	private final String[] args;
	private final List<Option> options = new ArrayList<>();

	public CLICommand getCommand() {
		return args.length < 1 ? null : CLICommand.fromCmdLineStr(args[0].trim());
	}
	
	public String getCommandStr() {
		return args.length < 1 ? null : args[0].trim(); 
	}

	public String getProductCoords() {
		return args.length < 2 ? null : args[1];
	}
	
	public CommandLine(String[] args) {
		this.args = args;
		if (args.length > 2) {
			String[] optionArgs = Arrays.copyOfRange(args, 2, args.length);
			for (String optionArg : optionArgs) {
				options.add(Option.fromCmdLineStr(optionArg.trim()));
			}
		}
	}
	
	private static String printOptions() {
		StringBuilder sb = new StringBuilder();
		for (Option opt : Option.values()) {
			sb.append("[").append(opt.getCmdLineStr()).append("] ");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	private static String printCommands() {
		StringBuilder sb = new StringBuilder();
		for (CLICommand cmd : CLICommand.values()) {
			if (cmd.getCmdLineStr() != null) {
				sb.append(cmd.getCmdLineStr() + "|");
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public static String getUsage() {
		int maxLen = CLICommand.getMaxLen();
		return "usage: groovy run.groovy " + printCommands() + " productCoords " + printOptions() + "\r\n"
				+ "\r\n"
				+ String.format("%-" + maxLen + "s - show actions will be made with product specified by productCoords\r\n", CLICommand.STATUS.getCmdLineStr())
				+ String.format("%-" + maxLen + "s - create all necessary release branches and lock mdeps versions for product specified by productCoords\r\n", CLICommand.FORK.getCmdLineStr())
				+ String.format("%-" + maxLen + "s - build a release of product specified by productCoords. The product must be forked before.", CLICommand.BUILD.getCmdLineStr()) + "\r\n"
				+ String.format("%-" + maxLen + "s   If " + Option.DELAYED_TAG.getCmdLineStr() + " option is provided then "
						+ "tag will not be applied to a new release commit. Use " + CLICommand.TAG.getCmdLineStr() + " to command to apply delayed tags\r\n", "")
				+ String.format("%-" + maxLen + "s - apply delayed tags on product specified by productCoords", CLICommand.TAG.getCmdLineStr())
				+ "\r\n"
				+ "If productCoords contains an exact version then this version will be used. Otherwise VCS versions will be used.\r\n"
				+ "Use " + Option.STACK_TRACE.getCmdLineStr() + " option to get full stack trace in case of any errors";
	}

	public String[] getOptionArgs() {
		if (args.length > 2) {
			return Arrays.copyOfRange(args, 2, args.length);
		}
		return new String[0];
	}

	public boolean isDelayedTag() {
		return options.contains(Option.DELAYED_TAG);
	}
}
