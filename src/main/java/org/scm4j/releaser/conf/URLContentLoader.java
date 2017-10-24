package org.scm4j.releaser.conf;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class URLContentLoader {
	
	private static final String URL_SEPARATOR = ";";
	public static final String DEFAULT_PROTOCOL = "file:///";

	public String getContentFromUrls(String separatedUrlsStr) throws IOException {
		if (separatedUrlsStr == null) {
			return null;
		}
		
		String[] urlsStrs = separatedUrlsStr.split(URL_SEPARATOR);
		StringBuilder mergedContent = new StringBuilder();
		for (String urlStr : urlsStrs) {
			mergedContent.append(getContentFromUrl(getWithDefaultProtocol(urlStr)));
		}
		return mergedContent.toString();
	}
	
	public String getWithDefaultProtocol(String urlStr) {
		if (!urlStr.trim().toLowerCase().startsWith("file:") && !urlStr.trim().toLowerCase().startsWith("http:") && !urlStr.trim().toLowerCase().startsWith("https:")) {
			return DEFAULT_PROTOCOL + urlStr;
		}
		return urlStr;
	}

	public String getContentFromUrl(String urlStr) throws IOException {
		if (urlStr == null) {
			return null;
		}
		URL url = new URL(getWithDefaultProtocol(urlStr));
		String vcsReposYml;
		try (InputStream inputStream = url.openStream()) {
			vcsReposYml = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
			return vcsReposYml;
		}
	}
	
}
