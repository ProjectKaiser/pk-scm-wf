package org.scm4j.releaser.conf;

import java.util.ArrayList;
import java.util.List;

public class Options {

	private static List<Option> options = new ArrayList<>();
	private static boolean isPatch;

	public static synchronized List<Option> getOptions() {
		return options;
	}

	public static boolean hasOption(Option option) {
		return getOptions().contains(option);
	}

	public static synchronized void setOptions(List<Option> options) {
		Options.options = options;
	}

	public static void parse(String[] strs) {
		List<Option> options = new ArrayList<>();
		for (String optionStr : strs) {
			options.add(Option.getArgsMap().get(optionStr));
		}
		setOptions(options);
	}
	
	public static void setIsPatch(boolean isPatch) {
		Options.isPatch = isPatch;
	}

	public static boolean isPatch() {
		return isPatch;
	}
}
