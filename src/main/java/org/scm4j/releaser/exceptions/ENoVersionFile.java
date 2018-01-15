package org.scm4j.releaser.exceptions;

import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.Component;

public class ENoVersionFile extends EReleaserException {

	private static final long serialVersionUID = 1L;

	public ENoVersionFile(Component comp) {
		super(Utils.VER_FILE_NAME + " file is missing in develop branch of " + comp);
	}
}
