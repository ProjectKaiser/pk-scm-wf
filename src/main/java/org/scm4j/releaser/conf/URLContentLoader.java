package org.scm4j.releaser.conf;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class URLContentLoader {
	
	private static final String URL_SEPARATOR = ";";

	public String getContentFromUrls(String separatedUrlsStr) throws IOException {
		if (separatedUrlsStr == null) {
			return null;
		}
		
		String[] urlsStrs = separatedUrlsStr.split(URL_SEPARATOR);
		StringBuilder mergedContent = new StringBuilder();
		for (String urlStr : urlsStrs) {
			mergedContent.append(getContentFromUrl(urlStr));
		}
		return mergedContent.toString();
	}
	
	public String getContentFromUrl(String urlStr) throws IOException {
		URL url = new URL(urlStr);
		String vcsReposYml;
		try (InputStream inputStream = url.openStream()) {
			vcsReposYml = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
			return vcsReposYml;
		}
	}
	
}
