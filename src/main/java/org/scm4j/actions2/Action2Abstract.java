package org.scm4j.actions2;

public abstract class Action2Abstract implements IAction2{
	
	private final String name;

	public Action2Abstract(String name){
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}

}
