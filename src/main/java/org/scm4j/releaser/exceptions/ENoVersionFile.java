package org.scm4j.releaser.exceptions;

import org.scm4j.releaser.Constants;
import org.scm4j.releaser.conf.Component;

public class ENoVersionFile extends EReleaserException {

	private static final long serialVersionUID = 1L;

	public ENoVersionFile(Component comp) {
		super(Constants.VER_FILE_NAME + " file is missing in develop branch of " + comp);
	}
}
