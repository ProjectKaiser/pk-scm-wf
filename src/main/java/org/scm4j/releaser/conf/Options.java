package org.scm4j.releaser.conf;

import java.util.ArrayList;
import java.util.List;

public final class Options {

	private List<Option> options = new ArrayList<>();
	
	public List<Option> getOptions() {
		return options;
	}

	public void parse(String[] strs) {
		options.clear();
		for (String optionStr : strs) {
			options.add(Option.getArgsMap().get(optionStr));
		}
	}
	
	public boolean isDelayedTag() {
		return options.contains(Option.DELAYED_TAG);
	}
}
