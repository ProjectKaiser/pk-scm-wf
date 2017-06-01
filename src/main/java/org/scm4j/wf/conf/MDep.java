package org.scm4j.wf.conf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MDep {

	private Version version;
	private String name;
	private String tail;
	private Boolean isEmpty;
	private String preName;
	private String comment;
	private String group;
	private String preVersion;
	private String major;
	private String minor;
	private Version ver;

	public MDep(String sourceString) {
		String str = sourceString;

		// комментарий
		String[] strs = str.split("#");
		if (strs.length > 1) {
			Integer commentPos = sourceString.indexOf("#");
			comment = str.substring(commentPos);
		}
		str = strs[0];
		
		strs = str.split(":");
		if (strs.length < 2) {
			throw new IllegalArgumentException("wrong mdep coord: " + sourceString);
		}
		
		group = strs[0].trim();
		StringBuilder sb = new StringBuilder();
		for (Integer i = 0; i <= group.length(); i++) {
			if (group.charAt(i) != name.charAt(0)) {
				sb.append(group.charAt(i));
			}
		}
		preName = sb.toString();
		
		name = strs[1];
		
		if (strs.length == 2) {
			return;
		}
		
		// если после имени нет версии, только классификатор
		if (strs[2].indexOf(".") < 0) {
			tail = strs[2];
			return;
		}
		
		// все что после версии
		if (strs.length > 3) {
			sb = new StringBuilder();
			String prefix = ":";
			for (Integer i = 3; i <= strs.length - 1; i++) {
				sb.append(prefix);
				sb.append(strs[i]);
			}
			tail = sb.toString();
		}
		
		// strs[2] - версия
		ver = new Version(strs[2]);
		
		
	}
	
	@Override
	public String toString() {
		return group + ":" + name + ":" + getVersion();
	}
	
	public String getVersion() {
		return preVersion + "." + major + "." + minor;
	}
	
	public String getMDepsString() {
		return preName + toString() + tail + "#" + comment;
	}
	

}
