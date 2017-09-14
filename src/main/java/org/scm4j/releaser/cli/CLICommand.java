package org.scm4j.releaser.cli;

import java.util.HashMap;
import java.util.Map;

public enum CLICommand {
	STATUS("status"), FORK("fork"), BUILD("build"), TAG("tag");
	
	private static final Map<String, CLICommand> map = new HashMap<>();
	private final String strValue;
	
	static {
		for (CLICommand cmd : values()) {
			map.put(cmd.getStrValue(), cmd);
		}
	}
	
	CLICommand(String strValue) {
		this.strValue = strValue;
	}
	
	public String getStrValue() {
		return strValue;
	}
	
	public static CLICommand fromStrValue(String strValue) {
		return map.get(strValue);
	}
}
