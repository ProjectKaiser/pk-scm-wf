package org.scm4j.progress;

import com.google.common.base.Strings;

public class ProgressConsole implements IProgress {

	private int level;
	private String name;
	private boolean singleLine;
	private String indent;
	private String outdent;

	public ProgressConsole(String name, String indent, String outdent) {
		this( 0, name, false, indent, outdent);
	}
	
	public ProgressConsole(String name) {
		this(0, name, false, "", "");
	}

	public ProgressConsole(String name, boolean singleLine) {
		this(0, name, singleLine, "", "");
	}

	public ProgressConsole(int level, String name, boolean singleLine, String indent, String outdent) {
		this.name = name;
		this.singleLine = singleLine;
		this.level = level;
		this.indent = indent;
		this.outdent = outdent;
		indent(level);
		print(indent + name);
		if (!singleLine)
			nl();
	}

	protected void print(Object s) {
		System.out.print(s.toString());
	}

	protected void nl() {
		print('\n');
	}

	protected void indent(int level) {
		print(Strings.repeat("\t", level));
	}

	@Override
	public IProgress createNestedProgress(String name) {
		return new ProgressConsole(level + 1, name, singleLine, indent, outdent);
	}

	@Override
	public void reportStatus(String status) {
		indent(level + 1);
		print(status);
		if (!singleLine) {
			nl();
		}
	}

	@Override
	public void close() {
		level--;
		reportStatus(outdent + name);
	}

}
