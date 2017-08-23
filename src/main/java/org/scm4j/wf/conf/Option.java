package org.scm4j.wf.conf;

import java.util.HashMap;
import java.util.Map;

public enum Option {
	DELAYED_TAG("--delayed-tag");
	
	private String strValue;
	
	private static Map<String, Option> map = new HashMap<String, Option>();
	
	static {
		for (Option option : Option.values()) {
			map.put(option.strValue, option);
		}
	}
	
	Option(String strValue) {
		this.strValue = strValue;
	}
	
	public String getStrValue() {
		return strValue;
	}
	
	public static Map<String, Option> getArgsMap() {
		return map;
	}
}
