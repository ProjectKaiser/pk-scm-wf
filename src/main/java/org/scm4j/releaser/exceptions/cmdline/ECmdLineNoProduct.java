package org.scm4j.releaser.exceptions.cmdline;

public class ECmdLineNoProduct extends ECmdLine {

	private static final long serialVersionUID = 1L;

	public ECmdLineNoProduct() {
		super("product is not specified");
	}

}
