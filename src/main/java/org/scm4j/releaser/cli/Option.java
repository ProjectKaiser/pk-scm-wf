package org.scm4j.releaser.cli;

import java.util.HashMap;
import java.util.Map;

public enum Option {
	UNKNOWN(null), DELAYED_TAG("--delayed-tag"), STACK_TRACE("--stacktrace");
	
	private static final Map<String, Option> map = new HashMap<>();

	static {
		for (Option option : Option.values()) {
			map.put(option.strValue, option);
		}
	}
	
	private final String strValue;
	
	Option(String strValue) {
		this.strValue = strValue;
	}
	
	public String getCmdLineStr() {
		return strValue;
	}
	
	public static Option fromCmdLineStr(String strValue) {
		Option res = map.get(strValue);
		return res == null ? UNKNOWN : res;
	}
}
