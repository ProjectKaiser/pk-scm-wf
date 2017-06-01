package org.scm4j.wf.conf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

public abstract class ConfFile {

	public static final String COMMENT_PREFIX = "#";
	private static final String KV_SEPARATOR = "=";

	public String toFileContent() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		for (Field field : this.getClass().getDeclaredFields()) {
			try {
				String methodName = "get" + field.getName().substring(0, 1).toUpperCase()
						+ field.getName().substring(1);
				String value = this.getClass().getMethod(methodName).invoke(this).toString();
				if (value != null) {
					pw.println(field.getName() + "=" + value);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return sw.toString();
	}
	
	public static String toFileContent(List<String> strs) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		for (String str : strs) {
			pw.println(str);
		}
		return sw.toString();
	}

	public ConfFile() {
	}

	public ConfFile(String confFileContent) {
		BufferedReader br = new BufferedReader(new StringReader(confFileContent));
		String str;
		try {
			str = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
		while (str != null) {
			String[] strs = str.split("#");
			parseLine(strs[0]);
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
}
