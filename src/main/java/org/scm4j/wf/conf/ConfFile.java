package org.scm4j.wf.conf;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

public abstract class ConfFile {

	public static final String SEP = "\n";
	public static final String COMMENT_PREFIX = "#";
	private static final String KV_SEPARATOR = "=";

	public String toFileContent() {
		StringBuilder sb = new StringBuilder();

		for (Field field : this.getClass().getDeclaredFields()) {
			try {
				String methodName = "get" + field.getName().substring(0, 1).toUpperCase()
						+ field.getName().substring(1);
				String value = this.getClass().getMethod(methodName).invoke(this).toString();
				if (value != null) {
					sb.append(field.getName() + "=" + value + SEP);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return sb.toString();
	}
	
	public static String toFileContent(List<String> strs) {
		StringBuilder sb = new StringBuilder();
		for (String str : strs) {
			sb.append(str + SEP);
		}
		return sb.toString();
	}

	public ConfFile() {
	}

	public ConfFile(String content) {
		String[] strs = content.split(SEP);
		for (String str : strs) {
			if (isComment(str)) {
				continue;
			}
			parseLine(str);
		}
	}

	protected void parseLine(String str) {
		Entry<String, String> entry = getEntry(str);
		try {
			String methodName = "set" + entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1);
			this.getClass().getDeclaredMethod(methodName, String.class).invoke(this, entry.getValue());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Entry<String, String> getEntry(String str) {
		String[] strs = str.split(KV_SEPARATOR);
		assert (strs.length == 2);
		return Maps.immutableEntry(strs[0].trim(), strs[1].trim());
	}

	private static Boolean isComment(String str) {
		return str.trim().startsWith(COMMENT_PREFIX);
	}
}
