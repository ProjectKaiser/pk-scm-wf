package org.scm4j.wf.conf;

public class TagDesc {

	private final String name;
	private final String message;

	public TagDesc(String name, String message) {
		this.name = name;
		this.message = message;
	}

	public String getName() {
		return name;
	}

	public String getMessage() {
		return message;
	}
}
