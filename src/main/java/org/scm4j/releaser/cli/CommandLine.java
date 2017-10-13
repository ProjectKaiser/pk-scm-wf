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
			sb.append("[" + opt.getStrValue() + "] ");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public static String getUsage() {
		return "usage:  groovy run.groovy status|fork|build|tag productCoords " + printOptions() + "\r\n" 
				+ "\r\n"
				+ "status:   show actions will be made with product specified by productCoords\r\n"
				+ "fork:     create all necessary release branches for product specified by productCoords\r\n"
				+ "build:    execute production release action on product specified by productCoords. If " + Option.DELAYED_TAG.getStrValue() + " option is provided then "
				+ "tag will not be applied to a new release commit. Use tag command to apply delayed tags\r\n"
				+ "tag:      apply delayed tags on product specified by productCoords"
				+ "\r\n"
				+ "If productCoords contains an exact version then this version will be used. Otherwise VCS versions will be used.\r\n"
				+ "Use " + Option.STACK_TRACE.getStrValue() + " option to get full stack trace in case of any errors";
	}
	
	public String[] getArgs() {
		return args;
	}
}
