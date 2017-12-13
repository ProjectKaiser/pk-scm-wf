package org.scm4j.releaser.cli;

import java.util.HashMap;
import java.util.Map;

public enum CLICommand {
	UNKNOWN(null), STATUS("status"), FORK("fork"), BUILD("build"), TAG("tag");
	
	private static final Map<String, CLICommand> map = new HashMap<>();
	private static final int maxLen;
	private final String strValue;
	
	static {
		int max = 0;
		for (CLICommand cmd : values()) {
			map.put(cmd.getCmdLineStr(), cmd);
			if (cmd.toString().length() > max) {
				max = cmd.toString().length();
			}
		}
		maxLen = max;
	}
	
	CLICommand(String strValue) {
		this.strValue = strValue;
	}
	
	public String getCmdLineStr() {
		return strValue;
	}
	
	public static CLICommand fromCmdLineStr(String strValue) {
		CLICommand res = map.get(strValue);
		return res == null ? UNKNOWN : res;
	}

	public static int getMaxLen() {
		return maxLen;
	}

}
