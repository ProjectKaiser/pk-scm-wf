package org.scm4j.releaser.cli;

import org.scm4j.releaser.conf.Option;

public class CommandLine {

	private final String[] args;

	public CLICommand getCommand() {
		return args.length < 1 ? null : CLICommand.fromStrValue(args[0]);
	}
	
	public String getCommandStr() {
		return args.length < 1 ? null : args[0]; 
	}

	public String getProductCoords() {
		return args.length < 2 ? null : args[1];
	}
	
	public CommandLine(String[] args) {
		this.args = args;
	}
	
	private static String printOptions() {
		StringBuilder sb = new StringBuilder();
		for (Option opt : Option.values()) {
			sb.append("[").append(opt.getStrValue()).append("] ");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	private static String printCommands() {
		StringBuilder sb = new StringBuilder();
		for (CLICommand cmd : CLICommand.values()) {
			sb.append(cmd.getStrValue() + "|");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public static String getUsage() {
		int maxLen = CLICommand.getMaxLen();
		return "usage:  groovy run.groovy " + printCommands() + " productCoords " + printOptions() + "\r\n"
				+ "\r\n"
				+ String.format("%-" + maxLen + "s - show actions will be made with product specified by productCoords\r\n", CLICommand.STATUS.getStrValue())
				+ String.format("%-" + maxLen + "s - create all necessary release branches for product specified by productCoords\r\n", CLICommand.FORK.getStrValue())
				+ String.format("%-" + maxLen + "s - execute production release action on product specified by productCoords.", CLICommand.BUILD.getStrValue()) + "\r\n"
				+ String.format("%-" + maxLen + "s   If " + Option.DELAYED_TAG.getStrValue() + " option is provided then "
						+ "tag will not be applied to a new release commit. Use " + CLICommand.TAG.getStrValue() + " to command to apply delayed tags\r\n", "")
				+ String.format("%-" + maxLen + "s - apply delayed tags on product specified by productCoords", CLICommand.TAG.getStrValue())
				+ "\r\n"
				+ "If productCoords contains an exact version then this version will be used. Otherwise VCS versions will be used.\r\n"
				+ "Use " + Option.STACK_TRACE.getStrValue() + " option to get full stack trace in case of any errors";
	}

	public String[] getArgs() {
		return args;
	}
}
