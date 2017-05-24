package org.scm4j.progress;

public class ProgressConsole implements IProgress{

	private IProgress parent;
	private int level;
	private String name;
	private boolean singleLine;

	public ProgressConsole(String name){
		this(null, 0, name, false);
	}
	
	public ProgressConsole(String name, boolean singleLine){
		this(null, 0, name, singleLine);
	}
	
	public ProgressConsole(IProgress parent, int level, String name, boolean singleLine){
		this.name = name;
		this.singleLine = singleLine;
		this.parent = parent;
		this.level = level;
		indent(level);
		print(name);
		if(!singleLine)nl();
	}

	protected void print(Object s){
		System.out.print(s);
	}
	
	protected void nl(){
		print('\n');		
	}
	
	protected void indent(int level){
		for (int i = 0; i < level; i++) {
			print('\t');
		}		
	}
	
	
	@Override
	public IProgress createNestedProgress(String name) {
		return new ProgressConsole(this, level + 1, name, singleLine);
	}
	
	@Override
	public void reportStatus(String status) {
		indent(level + 1);
		print(status);
		if(!singleLine){
			nl();
		}
	}

	@Override
	public IProgress getParent() {
		return parent;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void close() {
		if(singleLine){
			nl();
		}
	}

}
