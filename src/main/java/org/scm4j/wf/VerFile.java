package org.scm4j.wf;

import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

public class VerFile {

	private static final int DEFAULT_VERSION = 1;
	private static final Pattern LAST_NUMBER_PATTERN = Pattern.compile("[0-9]+$");
	public static final String SEP = "\n";
	public static final String COMMENT_PREFIX = "#";
	private static final String KV_SEPARATOR = "=";

	private String ver;
	private String release;

	public int getLastNumber() {
		Matcher matcher = LAST_NUMBER_PATTERN.matcher(ver);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(0));
		}
		return DEFAULT_VERSION;
	}

	public void setLastNumber(int number) {
		Matcher matcher = LAST_NUMBER_PATTERN.matcher(ver);
		if (matcher.find()) {
			ver = matcher.replaceFirst(Integer.toString(number));
		} else {
			ver = ver + Integer.toString(number);
		}
	}

	public String getVer() {
		return ver;
	}

	public void setVer(String ver) {
		this.ver = ver;
	}

	public String getRelease() {
		return release;
	}

	public void setRelease(String release) {
		this.release = release;
	}

	public VerFile() {
	}

	@Override
	public String toString() {
		return "VerFile [ver=" + ver + "]";
	}

	public String toFileContent() {
		StringBuilder sb = new StringBuilder();
		if (ver != null) {
			sb.append("ver=" + ver + SEP);
		}
		if (release != null) {
			sb.append("release=" + release + SEP);
		}
		return sb.toString();
	}

	public static VerFile fromFileContent(String content) {
		VerFile res = new VerFile();

		String[] strs = content.split(SEP);
		for (String str : strs) {
			if (isComment(str)) {
				continue;
			}
			Entry<String, String> entry = getEntry(str);
			if (entry.getKey().equals("ver")) {
				res.ver = entry.getValue();
			} else if (entry.getKey().equals("release")) {
				res.release = entry.getValue();
			}
		}
		return res;
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
