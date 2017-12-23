package org.scm4j.releaser.conf;

public class ConfigUrlsStrings implements IConfigUrls {

	private final String cc;
	private final String creds;

	public ConfigUrlsStrings(String cc, String creds) {
		this.cc = cc;
		this.creds = creds;
	}

	@Override
	public String getCCUrls() {
		return cc;
	}

	@Override
	public String getCredsUrl() {
		return creds;
	}
}
