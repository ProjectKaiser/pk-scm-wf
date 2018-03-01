package org.scm4j.releaser;

import java.io.File;

public final class Constants {

	public static final File RELEASES_DIR = new File(System.getProperty("user.dir"), "releases");
	public static final File BASE_WORKING_DIR = new File(System.getProperty("user.home"), ".scm4j");
	public static final String ZERO_PATCH = "0";
	public static final String VER_FILE_NAME = "version";
	public static final String MDEPS_FILE_NAME = "mdeps";

	public static final String SCM_VER = "#scm-ver";
	public static final String SCM_MDEPS = "#scm-mdeps";
	public static final String SCM_IGNORE = "#scm-ignore";
}
