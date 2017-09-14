package org.scm4j.releaser.cli;

import java.util.List;

import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.conf.Option;
import org.scm4j.releaser.exceptions.EConfig;

public class CommandLine {

	private final CLICommand command;
	private final List<Option> options;
	private final String productCoords;

	public CLICommand getCommand() {
		return command;
	}

	public List<Option> getOptions() {
		return options;
	}
	
	public String getProductCoords() {
		return productCoords;
	}
	
	public CommandLine(String[] args) throws EConfig {
		if (args == null || args.length < 2) {
			throw new EConfig ("too less parameters provided");
		}
		
		command = CLICommand.fromStrValue(args[0]);
		if (command == null) {
			throw new EConfig ("unknown command: " + args[0]);
		}
		
		productCoords = args[1];
		
		options = SCMReleaser.parseOptions(args);
	}

	public static String getUsage() {
		return "usage:  groovy run.groovy status|fork|build|tag productCoords [--delayed-tag]\r\n" 
				+ "\r\n"
				+ "status: show actions will be made with product specified by productCoords\r\n"
				+ "fork:   cerate all necessary release branches for product specified by productCoords\r\n"
				+ "build:  execute production release action on product specified by productCoords. If --delayed-tag option is provided then "
				+ "tag will not be applied to a new relase commit. Use tag command to apply delayed tags\r\n"
				+ "tag:    apply delayed tags on product specified by productCoords"
				+ "\r\n"
				+ "If productCoords contains an exact version then this version will be used. Otherwise VCS versions will be used.";
	}
}
