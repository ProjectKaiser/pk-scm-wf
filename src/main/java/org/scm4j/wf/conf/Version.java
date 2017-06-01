package org.scm4j.wf.conf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

public class Version {

	private static final int DEFAULT_VERSION = 0;
	private static final Pattern VERSION_PATTERN = Pattern.compile("[0-9]+");
	private static final int MINOR_POSITION = 1;
	private static final String SNAPSHOT = "-SNAPSHOT";
	
	private String ver;
	
	public List<Entry<String, Integer>> getOctets() {
		Matcher matcher = VERSION_PATTERN.matcher(ver);
		List<Entry<String, Integer>> res = new ArrayList<>();
		while(matcher.find()) {
			Entry<String, Integer> entry = Maps.immutableEntry(matcher.group(), matcher.start());
			res.add(entry);
		}
		return res;
	}
	
	public int getMinor() {
		List<Entry<String, Integer>> octets = getOctets();
		if (octets.size() > 1) {
			return Integer.parseInt(octets.get(MINOR_POSITION).getKey());
		}
		return DEFAULT_VERSION;
	}
	
	public Version(String ver) {
		this.ver = ver;
	}
	
	public void addSnapshot() {
		ver = ver + SNAPSHOT;
	}
	
	public void removeSnapshot() {
		ver = ver.replace(SNAPSHOT, "");
	}
	
	public void setVer(String ver) {
		this.ver = ver;
	}

	public void setMinor(int value) {
		List<Entry<String, Integer>> octets = getOctets();
		
		if (octets.size() > 1) { 
			Entry<String, Integer> entry = octets.get(MINOR_POSITION);
			ver = ver.substring(0, entry.getValue()) + Integer.toString(value) +
					ver.substring(entry.getValue() + entry.getKey().length(), ver.length());
		} else if (octets.size() == 0) {
			ver = Integer.toString(DEFAULT_VERSION) + "." + Integer.toString(value) + ".0." + ver;
		} else {
			Entry<String, Integer> entry = octets.get(0);
			ver = ver.substring(0, entry.getValue() + entry.getKey().length()) + "." + Integer.toString(value) + ".0." + ver.substring(
					entry.getValue() + entry.getKey().length(), ver.length() - 1);	
		}
	}

	@Override
	public String toString() {
		return ver;
	}

}
