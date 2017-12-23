package org.scm4j.releaser.conf;

import java.io.File;

public class ConfigUrlsFiles implements IConfigUrls {
	
	private final File reposFile;
	private final File credsFile;

	public ConfigUrlsFiles(File reposFile, File credsFile) {
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
