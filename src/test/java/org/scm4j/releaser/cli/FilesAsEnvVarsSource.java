package org.scm4j.releaser.cli;

import java.io.File;

public class FilesAsEnvVarsSource implements IEnvVarsSource {
	
	private final File reposFile;
	private final File credsFile;

	public FilesAsEnvVarsSource(File reposFile, File credsFile) {
		this.reposFile = reposFile;
		this.credsFile = credsFile;
	}

	@Override
	public String getCCUrls() {
		return getFileUri(reposFile);
		
	}

	private String getFileUri(File file) {
		if (file == null) {
			return null;
		}
		return file.toURI().toString();
	}

	@Override
	public String getCredsUrl() {
		return getFileUri(credsFile);
	}

}
