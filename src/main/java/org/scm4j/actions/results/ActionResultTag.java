package org.scm4j.actions.results;

import org.scm4j.vcs.api.VCSTag;

public class ActionResultTag {

	private final VCSTag tag;
	private final String name;

	public ActionResultTag(String name, VCSTag tag) {
		this.name = name;
		this.tag = tag;
	}

	public VCSTag getTag() {
		return tag;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "ActionResultTag [name=" + name + ", tag=" + tag + "]";
	}

}
