package org.scm4j.wf.conf;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public class URLContentLoader {
	
	private static final String URL_SEPARATOR = ";";

	public String getContentFromUrls(String separatedUrlsStr) throws MalformedURLException, IOException {
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
	
	public String getContentFromUrl(String urlStr) throws MalformedURLException, IOException {
		URL url = new URL(urlStr);
		String vcsReposYml;
		try (InputStream inputStream = url.openStream()) {
			vcsReposYml = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
			return vcsReposYml;
		}
	}
	
}
