package org.scm4j.progress;

import com.google.common.base.Strings;

public class ProgressConsole implements IProgress {

	private int level;
	private String name;
	private String indent;
	private String outdent;

	public ProgressConsole(String name, String indent, String outdent) {
		this( 0, name, indent, outdent);
	}
	
	public ProgressConsole(String name) {
		this(0, name, "", "");
	}

	public ProgressConsole(int level, String name, String indent, String outdent) {
		this.name = name;
		this.level = level;
		this.indent = indent;
		this.outdent = outdent;
		indent(level);
		print(indent + name);
	}

	protected void print(Object s) {
		System.out.print(s.toString());
		System.out.println();
	}

	protected void indent(int level) {
		System.out.print(Strings.repeat("\t", level));
	}

	@Override
	public IProgress createNestedProgress(String name) {
		return new ProgressConsole(level + 1, name, indent, outdent);
	}

	@Override
	public void reportStatus(String status) {
		indent(level + 1);
		print(status);
	}

	@Override
	public void close() {
		level--;
		reportStatus(outdent + name);
	}

}
