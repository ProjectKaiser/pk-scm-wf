package org.scm4j.actions2;

/**
 * 
 * Thrown by {@link Action2Error}
 * @author gmp
 *
 */
public class EAction2Error extends RuntimeException{
	private static final long serialVersionUID = 1L;
	
	public EAction2Error(String name, String reason) {
		super(name + reason == null? "" : ": " + reason);			
	}
}
